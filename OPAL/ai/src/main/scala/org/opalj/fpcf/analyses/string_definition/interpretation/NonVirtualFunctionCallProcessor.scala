/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation.UnknownWordSymbol
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.StringTreeConst
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

/**
 * This implementation of [[AbstractExprProcessor]] processes
 * [[org.opalj.tac.NonVirtualFunctionCall]] expressions.
 * Currently, this implementation is only a rough approximation in the sense that all
 * `NonVirtualFunctionCall`s are processed by returning a [[StringTreeConst]] with no children
 * and `StringConstancyProperty(DYNAMIC, ArrayBuffer("*"))` as a value (i.e., it does not analyze
 * the function call in depth).
 *
 * @author Patrick Mell
 */
class NonVirtualFunctionCallProcessor() extends AbstractExprProcessor {

    /**
     * `expr` of `assignment`is required to be of type [[org.opalj.tac.NonVirtualFunctionCall]]
     * (otherwise `None` will be returned).
     * `stmts` currently is not relevant, thus an empty array may be passed.
     *
     * @see [[AbstractExprProcessor.processAssignment]]
     */
    override def processAssignment(
        assignment: Assignment[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(assignment.expr, stmts, ignore)

    /**
     * `expr` is required to be of type [[org.opalj.tac.NonVirtualFunctionCall]] (otherwise `None`
     * will be returned). `stmts` currently is not relevant, thus an empty array may be passed.
     *
     * @see [[AbstractExprProcessor.processExpr()]]
     */
    override def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(expr, stmts, ignore)

    /**
     * Wrapper function for processing.
     */
    private def process(
        expr: Expr[V], stmts: Array[Stmt[V]], ignore: List[Int]
    ): Option[StringTree] = {
        expr match {
            case _: NonVirtualFunctionCall[V] ⇒ Some(StringTreeConst(
                StringConstancyInformation(DYNAMIC, UnknownWordSymbol)
            ))
            case _ ⇒ None
        }
    }

}