/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

/**
 * Common interface of all code optimizers that operate on the three-address code
 * representation.
 *
 * @author Michael Eichberg
 */
trait TACOptimization[P <: AnyRef, V <: Var[V], C <: TACode[P, V]] {

    /**
     * Transforms the given code to the target code.
     */
    def apply(tac: TACOptimizationResult[P, V, C]): TACOptimizationResult[P, V, C]
}

/**
 * Encapsulates the result of an optimization/transformation of some three-address code.
 *
 * @author Michael Eichberg
 */
case class TACOptimizationResult[P <: AnyRef, V <: Var[V], C <: TACode[P, V]](
        code:           C,
        wasTransformed: Boolean
)
