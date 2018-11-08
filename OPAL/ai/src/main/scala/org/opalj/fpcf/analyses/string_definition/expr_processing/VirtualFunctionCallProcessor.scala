/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation.UnknownWordSymbol
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.TreeConditionalElement
import org.opalj.fpcf.string_definition.properties.TreeElement
import org.opalj.fpcf.string_definition.properties.TreeValueElement
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Expr
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable.ListBuffer

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.VirtualFunctionCall]]
 * expressions.
 * Currently, [[VirtualFunctionCallProcessor]] (only) aims at processing calls of
 * [[StringBuilder#append]].
 *
 * @author Patrick Mell
 */
class VirtualFunctionCallProcessor(
        private val exprHandler: ExprHandler,
        private val cfg:         CFG[Stmt[V], TACStmts[V]]
) extends AbstractExprProcessor {

    /**
     * `expr` of `assignment`is required to be of type [[org.opalj.tac.VirtualFunctionCall]]
     * (otherwise `None` will be returned).
     *
     * @see [[AbstractExprProcessor.processAssignment]]
     */
    override def processAssignment(
        assignment: Assignment[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(assignment.expr, stmts, ignore)

    /**
     * This implementation does not change / implement the behavior of
     * [[AbstractExprProcessor.processExpr]].
     */
    override def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(expr, stmts, ignore)

    /**
     * Wrapper function for processing expressions.
     */
    private def process(
        expr: Expr[V], stmts: Array[Stmt[V]], ignore: List[Int]
    ): Option[StringTree] = {
        expr match {
            case vfc: VirtualFunctionCall[V] ⇒
                if (ExprHandler.isStringBuilderAppendCall(expr)) {
                    Some(processAppendCall(vfc, stmts, ignore))
                } else if (ExprHandler.isStringBuilderToStringCall(expr)) {
                    processToStringCall(vfc, stmts, ignore)
                } // A call to method which is not (yet) supported
                else {
                    val ps = ExprHandler.classNameToPossibleString(
                        vfc.descriptor.returnType.toJavaClass.getSimpleName
                    )
                    Some(TreeValueElement(None, StringConstancyInformation(DYNAMIC, ps)))
                }
            case _ ⇒ None
        }
    }

    /**
     * Function for processing calls to [[StringBuilder#append]].
     */
    private def processAppendCall(
        call: VirtualFunctionCall[V], stmts: Array[Stmt[V]], ignore: List[Int]
    ): TreeElement = {
        val defSites = call.receiver.asVar.definedBy.filter(!ignore.contains(_))
        val appendValue = valueOfAppendCall(call, stmts)
        if (defSites.isEmpty) {
            appendValue
        } else {
            val upperTree = exprHandler.processDefSites(defSites).get
            upperTree.getLeafs.foreach { _.child = Some(appendValue) }
            upperTree
        }
    }

    /**
     * Function for processing calls to [[StringBuilder.toString]].
     */
    private def processToStringCall(
        call: VirtualFunctionCall[V], stmts: Array[Stmt[V]], ignore: List[Int]
    ): Option[StringTree] = {
        val children = ListBuffer[TreeElement]()
        val defSites = call.receiver.asVar.definedBy.filter(!ignore.contains(_))
        defSites.foreach {
            exprHandler.processDefSite(_) match {
                case Some(subtree) ⇒ children.append(subtree)
                case None          ⇒
            }
        }

        children.size match {
            case 0 ⇒ None
            case 1 ⇒ Some(children.head)
            case _ ⇒ Some(TreeConditionalElement(children))
        }
    }

    /**
     * Determines the string value that was passed to a `StringBuilder#append` method. This function
     * can process string constants as well as function calls as argument to append.
     *
     * @param call  A function call of `StringBuilder#append`. Note that for all other methods an
     *              [[IllegalArgumentException]] will be thrown.
     * @param stmts The surrounding context, e.g., the surrounding method.
     * @return Returns a [[TreeValueElement]] with no children and the following value for
     *         [[StringConstancyInformation]]: For constants strings as arguments, this function
     *         returns the string value and the level
     *         [[org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT]]. For
     *         function calls "*" (to indicate ''any value'') and
     *         [[org.opalj.fpcf.string_definition.properties.StringConstancyLevel.DYNAMIC]].
     */
    private def valueOfAppendCall(
        call: VirtualFunctionCall[V], stmts: Array[Stmt[V]]
    ): TreeValueElement = {
        val defAssignment = call.params.head.asVar.definedBy.head
        val assign = stmts(defAssignment).asAssignment
        val sci = assign.expr match {
            case _: NonVirtualFunctionCall[V] ⇒
                StringConstancyInformation(DYNAMIC, UnknownWordSymbol)
            case StringConst(_, value) ⇒
                StringConstancyInformation(CONSTANT, value)
            // Next case is for an append call as argument to append
            case _: VirtualFunctionCall[V] ⇒
                processAssignment(assign, stmts, cfg).get.reduce()
            case be: BinaryExpr[V] ⇒
                val possibleString = ExprHandler.classNameToPossibleString(
                    be.left.asVar.value.getClass.getSimpleName
                )
                StringConstancyInformation(DYNAMIC, possibleString)
        }
        TreeValueElement(None, sci)
    }

}
