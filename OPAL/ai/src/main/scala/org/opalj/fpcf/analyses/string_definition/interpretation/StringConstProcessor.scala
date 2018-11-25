/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.StringTreeConst
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.StringConst]]
 * expressions.
 *
 * @author Patrick Mell
 */
class StringConstProcessor() extends AbstractExprProcessor {

    /**
     * For this implementation, `stmts` is not required (thus, it is safe to pass an empty value).
     * The `expr` of `assignment` is required to be of type [[org.opalj.tac.StringConst]] (otherwise
     * `None` will be returned).
     *
     * @note The sub-tree, which is created by this implementation, does not have any children.
     * @see [[AbstractExprProcessor.processAssignment]]
     */
    override def processAssignment(
        assignment: Assignment[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(assignment.expr, stmts, ignore)

    /**
     * For this implementation, `stmts` is not required (thus, it is safe to pass an empty value).
     * `expr`  is required to be of type [[org.opalj.tac.StringConst]] (otherwise `None` will be
     * returned).
     *
     * @note The sub-tree, which is created by this implementation, does not have any children.
     * @see [[AbstractExprProcessor.processExpr()]]
     */
    override def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = process(expr, stmts, ignore)

    /**
     * Wrapper function for processing an expression.
     */
    private def process(
        expr: Expr[V], stmts: Array[Stmt[V]], ignore: List[Int]
    ): Option[StringTree] = {
        expr match {
            case strConst: StringConst ⇒ Some(StringTreeConst(
                StringConstancyInformation(CONSTANT, strConst.value)
            ))
            case _ ⇒ None
        }
    }

}
