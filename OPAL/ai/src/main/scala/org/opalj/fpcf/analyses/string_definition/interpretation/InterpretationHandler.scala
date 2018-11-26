/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * `InterpretationHandler` is responsible for processing expressions that are relevant in order to
 * determine which value(s) a string read operation might have. These expressions usually come from
 * the definitions sites of the variable of interest.
 *
 * @param cfg The control flow graph that underlies the program / method in which the expressions of
 *            interest reside.
 * @author Patrick Mell
 */
class InterpretationHandler(cfg: CFG[Stmt[V], TACStmts[V]]) {
    private val stmts = cfg.code.instructions
    private val processedDefSites = ListBuffer[Int]()

    /**
     * Processes a given definition site. That is, this function determines the interpretation of
     * the specified instruction.
     *
     * @param defSite The definition site to process. Make sure that (1) the value is >= 0, (2) it
     *                actually exists, and (3) can be processed by one of the subclasses of
     *                [[AbstractStringInterpreter]] (in case (3) is violated, an
     *                [[IllegalArgumentException]] will be thrown.
     * @return Returns a list of interpretations in the form of [[StringConstancyInformation]]. In
     *         case the rules listed above or the ones of the different processors are not met, an
     *         empty list will be returned.
     */
    def processDefSite(defSite: Int): List[StringConstancyInformation] = {
        if (defSite < 0 || processedDefSites.contains(defSite)) {
            return List()
        }
        processedDefSites.append(defSite)

        stmts(defSite) match {
            case Assignment(_, _, expr) if expr.isInstanceOf[StringConst] ⇒
                new StringConstInterpreter(cfg, this).interpret(expr.asStringConst)
            case Assignment(_, _, expr) if expr.isInstanceOf[ArrayLoad[V]] ⇒
                new ArrayLoadInterpreter(cfg, this).interpret(expr.asArrayLoad)
            case Assignment(_, _, expr) if expr.isInstanceOf[New] ⇒
                new NewInterpreter(cfg, this).interpret(expr.asNew)
            case Assignment(_, _, expr) if expr.isInstanceOf[VirtualFunctionCall[V]] ⇒
                new VirtualFunctionCallInterpreter(cfg, this).interpret(expr.asVirtualFunctionCall)
            case Assignment(_, _, expr) if expr.isInstanceOf[BinaryExpr[V]] ⇒
                new BinaryExprInterpreter(cfg, this).interpret(expr.asBinaryExpr)
            case Assignment(_, _, expr) if expr.isInstanceOf[NonVirtualFunctionCall[V]] ⇒
                new NonVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr.asNonVirtualFunctionCall)
            case ExprStmt(_, expr) ⇒
                expr match {
                    case vfc: VirtualFunctionCall[V] ⇒
                        new VirtualFunctionCallInterpreter(cfg, this).interpret(vfc)
                    case _ ⇒ List()
                }
            case nvmc: NonVirtualMethodCall[V] ⇒
                new NonVirtualMethodCallInterpreter(cfg, this).interpret(nvmc)
            case _ ⇒ List()

        }
    }

    /**
     * This function serves as a wrapper function for [[InterpretationHandler.processDefSite]] in the
     * sense that it processes multiple definition sites. Thus, it may throw an exception as well if
     * an expression referenced by a definition site cannot be processed. The same rules as for
     * [[InterpretationHandler.processDefSite]] apply.
     *
     * @param defSites The definition sites to process.
     * @return Returns a list of lists of [[StringConstancyInformation]]. Note that this function
     *         preserves the order of the given `defSites`, i.e., the first element in the result
     *         list corresponds to the first element in `defSites` and so on. If a site could not be
     *         processed, the list for that site will be the empty list.
     */
    def processDefSites(defSites: Array[Int]): List[List[StringConstancyInformation]] =
        defSites.length match {
            case 0 ⇒ List()
            case 1 ⇒ List(processDefSite(defSites.head))
            case _ ⇒ defSites.filter(_ >= 0).map(processDefSite).toList
        }

    /**
     * The [[InterpretationHandler]] keeps an internal state for correct and faster processing. As long as a
     * single object within a CFG is analyzed, there is no need to reset the state. However, when
     * analyzing a second object (even the same object) it is necessary to call `reset` to reset the
     * internal state. Otherwise, incorrect results will be produced.
     * (Alternatively, you could instantiate another [[InterpretationHandler]] instance.)
     */
    def reset(): Unit = {
        processedDefSites.clear()
    }

}

object InterpretationHandler {

    /**
     * @see [[InterpretationHandler]]
     */
    def apply(cfg: CFG[Stmt[V], TACStmts[V]]): InterpretationHandler = new InterpretationHandler(cfg)

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
     * Determines the definition site of the initialization of the base object that belongs to a
     * ''toString'' call.
     *
     * @param toString The ''toString'' call of the object for which to get the initialization def
     *                 site for. Make sure that the object is a subclass of
     *                 [[AbstractStringBuilder]].
     * @param stmts A list of statements which will be used to lookup which one the initialization
     *              is.
     * @return Returns the definition site of the base object of the call. If something goes wrong,
     *         e.g., no initialization is found, ''None'' is returned.
     */
    def findDefSiteOfInit(toString: VirtualFunctionCall[V], stmts: Array[Stmt[V]]): List[Int] = {
        // TODO: Check that we deal with an instance of AbstractStringBuilder
        if (toString.name != "toString") {
            return List()
        }

        val defSites = ListBuffer[Int]()
        val stack = mutable.Stack[Int](toString.receiver.asVar.definedBy.toArray: _*)
        while (stack.nonEmpty) {
            val next = stack.pop()
            stmts(next) match {
                case a: Assignment[V] ⇒
                    a.expr match {
                        case _: New ⇒
                            defSites.append(next)
                        case vfc: VirtualFunctionCall[V] ⇒
                            stack.pushAll(vfc.receiver.asVar.definedBy.toArray)
                    }
                case _ ⇒
            }
        }

        defSites.sorted.toList
    }

}
