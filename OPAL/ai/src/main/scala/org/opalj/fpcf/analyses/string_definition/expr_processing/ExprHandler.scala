/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.TreeConditionalElement
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable.ListBuffer

/**
 * `ExprHandler` is responsible for processing expressions that are relevant in order to determine
 * which value(s) a string read operation might have. These expressions usually come from the
 * definitions sites of the variable of interest.
 *
 * @param p The project associated with the analysis.
 * @param m The [[Method]] in which the read statement of the string variable of interest occurred.
 * @author Patrick Mell
 */
class ExprHandler(p: SomeProject, m: Method) {

    private val tacProvider = p.get(SimpleTACAIKey)
    private val ctxStmts = tacProvider(m).stmts
    private val processedDefSites = ListBuffer[Int]()
    private val cfg = tacProvider(m).cfg

    /**
     * Processes a given definition site. That is, this function determines the
     * [[StringTree]] of a string definition.
     *
     * @param defSite The definition site to process. Make sure that (1) the value is >= 0, (2) it
     *                actually exists, and (3) contains an Assignment whose expression is of a type
     *                that is supported by a sub-class of [[AbstractExprProcessor]].
     * @return Returns a StringTee that describes the definition at the specified site. In case the
     *         rules listed above or the ones of the different processors are not met `None` will be
     *         returned.
     */
    def processDefSite(defSite: Int): Option[StringTree] = {
        if (defSite < 0 || processedDefSites.contains(defSite)) {
            return None
        }
        processedDefSites.append(defSite)

        // Determine whether to process an assignment or an expression
        val expr = ctxStmts(defSite) match {
            case a: Assignment[V] ⇒ a.expr
            case e: ExprStmt[V]   ⇒ e.expr
            case _ ⇒ throw new IllegalArgumentException(
                s"cannot process ${ctxStmts(defSite)}"
            )
        }
        val exprProcessor: AbstractExprProcessor = expr match {
            case _: ArrayLoad[V]              ⇒ new ArrayLoadProcessor(this)
            case _: VirtualFunctionCall[V]    ⇒ new VirtualFunctionCallProcessor(this, cfg)
            case _: New                       ⇒ new NewStringBuilderProcessor(this)
            case _: NonVirtualFunctionCall[V] ⇒ new NonVirtualFunctionCallProcessor()
            case _: StringConst               ⇒ new StringConstProcessor()
            case _ ⇒ throw new IllegalArgumentException(
                s"cannot process expression $expr"
            )
        }

        val subtree = ctxStmts(defSite) match {
            case a: Assignment[V] ⇒
                exprProcessor.processAssignment(a, ctxStmts, cfg, processedDefSites.toList)
            case _ ⇒
                exprProcessor.processExpr(expr, ctxStmts, cfg, processedDefSites.toList)
        }
        subtree
    }

    /**
     * This function serves as a wrapper function for [[ExprHandler.processDefSite]] in the
     * sense that it processes multiple definition sites. Thus, it may throw an exception as well if
     * an expression referenced by a definition site cannot be processed. The same rules as for
     * [[ExprHandler.processDefSite]] apply.
     *
     * @param defSites The definition sites to process.
     * @return Returns a [[StringTree]]. In contrast to [[ExprHandler.processDefSite]] this function
     *         takes into consideration only those values from `processDefSite` that are not `None`.
     *         Furthermore, this function assumes that different definition sites originate from
     *         control flow statements; thus, this function returns a tree with a
     *         [[TreeConditionalElement]] as root and
     *         each definition site as a child.
     */
    def processDefSites(defSites: IntTrieSet): Option[StringTree] =
        defSites.size match {
            case 0 ⇒ None
            case 1 ⇒ processDefSite(defSites.head)
            case _ ⇒
                val processedSites = defSites.filter(_ >= 0).map(processDefSite _)
                Some(TreeConditionalElement(
                    processedSites.filter(_.isDefined).map(_.get).to[ListBuffer]
                ))
        }

}

object ExprHandler {

    private val classNameMap = Map(
        "AnIntegerValue" → "[AnIntegerValue]",
        "int" → "[AnIntegerValue]"
    )

    /**
     * @see [[ExprHandler]]
     */
    def apply(p: SomeProject, m: Method): ExprHandler = new ExprHandler(p, m)

    /**
     * Checks whether an expression contains a call to [[StringBuilder.toString]].
     *
     * @param expr The expression that is to be checked.
     * @return Returns true if `expr` is a call to [[StringBuilder.toString]].
     */
    def isStringBuilderToStringCall(expr: Expr[V]): Boolean =
        expr match {
            case VirtualFunctionCall(_, clazz, _, name, _, _, _) ⇒
                clazz.toJavaClass.getName == "java.lang.StringBuilder" && name == "toString"
            case _ ⇒ false
        }

    /**
     * Checks whether an expression is a call to [[StringBuilder#append]].
     *
     * @param expr The expression that is to be checked.
     * @return Returns true if `expr` is a call to [[StringBuilder#append]].
     */
    def isStringBuilderAppendCall(expr: Expr[V]): Boolean =
        expr match {
            case VirtualFunctionCall(_, clazz, _, name, _, _, _) ⇒
                clazz.toJavaClass.getName == "java.lang.StringBuilder" && name == "append"
            case _ ⇒ false
        }

    /**
     * Retrieves the definition sites of the receiver of a [[StringBuilder.toString]] call.
     *
     * @param expr The expression that contains the receiver whose definition sites to get.
     * @return If `expr` does not conform to the expected structure, an [[EmptyIntTrieSet]] is
     *         returned (avoid by using [[isStringBuilderToStringCall]]) and otherwise the
     *         definition sites of the receiver.
     */
    def getDefSitesOfToStringReceiver(expr: Expr[V]): IntTrieSet =
        if (!isStringBuilderToStringCall(expr)) {
            EmptyIntTrieSet
        } else {
            expr.asVirtualFunctionCall.receiver.asVar.definedBy
        }

    /**
     * Maps a class name to a string which is to be displayed as a possible string.
     *
     * @param javaSimpleClassName The simple class name, i.e., NOT fully-qualified, for which to
     *                            retrieve the value for "possible string".
     * @return Either returns the mapped string representation or, when an unknown string is passed,
     *         the passed parameter surrounded by "[" and "]".
     */
    def classNameToPossibleString(javaSimpleClassName: String): String =
        classNameMap.getOrElse(javaSimpleClassName, s"[$javaSimpleClassName]")

}
