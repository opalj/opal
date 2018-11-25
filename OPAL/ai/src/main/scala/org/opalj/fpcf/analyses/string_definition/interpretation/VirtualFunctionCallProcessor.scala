/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

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
    ): Option[StringTree] = process(assignment.expr, Some(assignment), stmts, ignore)

    /**
     * @see [[AbstractExprProcessor.processExpr]].
     *
     * @note For expressions, some information are not available that an [[Assignment]] captures.
     *       Nonetheless, as much information as possible is extracted from this implementation (but
     *       no use sites for `append` calls, for example).
     */
    override def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(expr, None, stmts, ignore)

    /**
     * Wrapper function for processing assignments.
     */
    private def process(
        expr: Expr[V], assignment: Option[Assignment[V]], stmts: Array[Stmt[V]], ignore: List[Int]
    ): Option[StringTree] = {
        None
        //        expr match {
        //            case vfc: VirtualFunctionCall[V] ⇒
        //                if (ExprHandler.isStringBuilderAppendCall(expr)) {
        //                    processAppendCall(expr, assignment, stmts, ignore)
        //                } else if (ExprHandler.isStringBuilderToStringCall(expr)) {
        //                    processToStringCall(assignment, stmts, ignore)
        //                } // A call to method which is not (yet) supported
        //                else {
        //                    val ps = ExprHandler.classNameToPossibleString(
        //                        vfc.descriptor.returnType.toJavaClass.getSimpleName
        //                    )
        //                    Some(StringTreeConst(StringConstancyInformation(DYNAMIC, ps)))
        //                }
        //            case _ ⇒ None
        //        }
    }

    /**
     * Function for processing calls to [[StringBuilder#append]].
     */
    //    private def processAppendCall(
    //        expr: Expr[V], assignment: Option[Assignment[V]], stmts: Array[Stmt[V]], ignore: List[Int]
    //    ): Option[StringTreeElement] = {
    //        val defSites = expr.asVirtualFunctionCall.receiver.asVar.definedBy.toArray.sorted
    //        val appendValue = valueOfAppendCall(expr.asVirtualFunctionCall, stmts, ignore)
    //        // Append has been processed before => do not compute again
    //        if (appendValue.isEmpty) {
    //            return None
    //        }
    //
    //        val leftSiblings = exprHandler.processDefSites(defSites)
    //        // For assignments, we can take use sites into consideration as well
    //        var rightSiblings: Option[StringTree] = None
    //        if (assignment.isDefined) {
    //            val useSites = assignment.get.targetVar.asVar.usedBy.toArray.sorted
    //            rightSiblings = exprHandler.processDefSites(useSites)
    //        }
    //
    //        if (leftSiblings.isDefined || rightSiblings.isDefined) {
    //            // Combine siblings and return
    //            val concatElements = ListBuffer[StringTreeElement]()
    //            if (leftSiblings.isDefined) {
    //                concatElements.append(leftSiblings.get)
    //            }
    //            concatElements.append(appendValue.get)
    //            if (rightSiblings.isDefined) {
    //                concatElements.append(rightSiblings.get)
    //            }
    //            Some(StringTreeConcat(concatElements))
    //        } else {
    //            Some(appendValue.get)
    //        }
    //    }

    /**
     * Function for processing calls to [[StringBuilder.toString]]. Note that a value not equals to
     * `None` can only be expected if `assignments` is defined.
     */
    //    private def processToStringCall(
    //        assignment: Option[Assignment[V]], stmts: Array[Stmt[V]], ignore: List[Int]
    //    ): Option[StringTree] = {
    //        if (assignment.isEmpty) {
    //            return None
    //        }
    //
    //        val children = ListBuffer[StringTreeElement]()
    //        val call = assignment.get.expr.asVirtualFunctionCall
    //        val defSites = call.receiver.asVar.definedBy.filter(!ignore.contains(_))
    //        defSites.foreach {
    //            exprHandler.processDefSite(_) match {
    //                case Some(subtree) ⇒ children.append(subtree)
    //                case None          ⇒
    //            }
    //        }
    //
    //        children.size match {
    //            case 0 ⇒ None
    //            case 1 ⇒ Some(children.head)
    //            case _ ⇒ Some(StringTreeCond(children))
    //        }
    //    }

    /**
     * Determines the string value that was passed to a `StringBuilder#append` method. This function
     * can process string constants as well as function calls as argument to append.
     *
     * @param call  A function call of `StringBuilder#append`. Note that for all other methods an
     *              [[IllegalArgumentException]] will be thrown.
     * @param stmts The surrounding context, e.g., the surrounding method.
     * @return Returns a [[org.opalj.fpcf.string_definition.properties.StringTreeConst]] with no children and the following value for
     *         [[org.opalj.fpcf.string_definition.properties.StringConstancyInformation]]: For constants strings as arguments, this function
     *         returns the string value and the level
     *         [[org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT]]. For
     *         function calls "*" (to indicate ''any value'') and
     *         [[org.opalj.fpcf.string_definition.properties.StringConstancyLevel.DYNAMIC]].
     */
    //    private def valueOfAppendCall(
    //        call: VirtualFunctionCall[V], stmts: Array[Stmt[V]], ignore: List[Int]
    //    ): Option[StringTreeConst] = {
    //        val defAssignment = call.params.head.asVar.definedBy.head
    //        // The definition has been seen before => do not recompute
    //        if (ignore.contains(defAssignment)) {
    //            return None
    //        }
    //
    //        val assign = stmts(defAssignment).asAssignment
    //        val sci = assign.expr match {
    //            case _: NonVirtualFunctionCall[V] ⇒
    //                StringConstancyInformation(DYNAMIC, UnknownWordSymbol)
    //            case StringConst(_, value) ⇒
    //                StringConstancyInformation(CONSTANT, value)
    //            // Next case is for an append call as argument to append
    //            case _: VirtualFunctionCall[V] ⇒
    //                processAssignment(assign, stmts, cfg).get.reduce()
    //            case be: BinaryExpr[V] ⇒
    //                val possibleString = ExprHandler.classNameToPossibleString(
    //                    be.left.asVar.value.getClass.getSimpleName
    //                )
    //                StringConstancyInformation(DYNAMIC, possibleString)
    //        }
    //        Some(StringTreeConst(sci))
    //    }

}
