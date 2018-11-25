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
 * AbstractExprProcessor defines the abstract / general strategy to process expressions in the
 * context of string definition analyses. Different sub-classes process different kinds of
 * expressions. The idea is to transform expressions into [[StringTree]] objects. For example, the
 * expression of a constant assignment might be processed.
 *
 * @author Patrick Mell
 */
abstract class AbstractExprProcessor() {

    /**
     * Implementations process an assignment which is supposed to yield a string tree.
     *
     * @param assignment  The Assignment to process. Make sure that the assignment, which is
     *                    passed, meets the requirements of that implementation.
     * @param stmts The statements that surround the expression to process, such as a method.
     *              Concrete processors might use these to retrieve further information.
     * @param cfg The control flow graph that corresponds to the given `stmts`
     * @param ignore A list of processed def or use sites. This list makes sure that an assignment
     *               or expression is not processed twice (which could lead to duplicate
     *               computations and unnecessary elements in the resulting string tree.
     * @return Determines the [[StringTree]] for the given `expr` and `stmts` from which possible
     *         string values, which the expression might produce, can be derived. If `expr` does not
     *         meet the requirements of a an implementation, `None` will be returned (or in severe
     *         cases an exception be thrown).
     * @see StringConstancyProperty
     */
    def processAssignment(
        assignment: Assignment[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree]

    /**
     * Implementations process an expression which is supposed to yield a string tree.
     *
     * @param expr   The [[Expr]] to process. Make sure that the expression, which is passed, meets
     *               the requirements of the corresponding implementation.
     * @param stmts  The statements that surround the expression to process, such as a method.
     *               Concrete processors might use these to retrieve further information.
     * @param ignore A list of processed def or use sites. This list makes sure that an assignment
     *               or expression is not processed twice (which could lead to duplicate
     *               computations and unnecessary elements in the resulting string tree.
     * @return Determines the [[StringTree]] for the given `expr` from which possible string values,
     *         which the expression might produce, can be derived. If `expr` does not
     *         meet the requirements of a an implementation, `None` will be returned (or in severe
     *         cases an exception be thrown).
     *
     * @note Note that implementations of [[AbstractExprProcessor]] are not required to implement
     *       this method (by default, `None` will be returned.
     */
    def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = { None }

}
