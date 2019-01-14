/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.value.ValueInformation

/**
 * Defines nodes used by statements and expressions.
 *
 * @author Michael Eichberg
 */
trait ASTNode[+V <: Var[V]] {

    /**
     * Each type of node is assigned a different `id` to make it easily possible
     * to do a switch over all nodes.
     */
    def astID: Int

    /**
     * `true` if the statement/expression is ''GUARANTEED'' to have no externally observable
     * effect if it is not executed.
     * Sideeffect free instructions can be removed if the result of the evaluation of the
     * expression/statement is not used. For those instructions, which may result in an exception, it
     * has to be guaranteed that the exception is '''NEVER''' thrown. For example, a div instruction
     * is sideeffect free if it is (statically) known that the divisor is always not equal to zero;
     * otherwise, even if the result value is not used, the expression is not (potentially) side
     * effect free. An array load is only side effect free if the array reference is non-null and
     * if the index is valid.
     *
     * @note '''Deeply nested expressions are not supported'''; i.e. an expression's sub-expressions
     *       have to be [[Var]] or [[Const]] expressions. Generally, a statements expressions have to
     *       to simple expressions too - except of the [[Assignment]] statement; in the latter case
     *       the right-expression can have references to simple expressions. Hence, in case of
     *       [[Assignment]] statements the side-effect freenes is determined by the referenced
     *       expression; in all other cases the side-effect freeness is determined directly by
     *       the statement/expression.
     *
     * @return `true` if the expression is ''GUARENTEED'' to have no side effect other than
     *        wasting some CPU cycles if it is not executed.
     */
    def isSideEffectFree: Boolean

    /** See [[org.opalj.value.ValueInformation.toCanonicalForm]] for detail. */
    def toCanonicalForm(
        implicit
        ev: V <:< DUVar[ValueInformation]
    ): ASTNode[DUVar[ValueInformation]]

}

