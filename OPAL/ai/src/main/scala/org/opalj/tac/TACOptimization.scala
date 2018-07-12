/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

/**
 * Common interface of all code optimizers that operate on the three-address code
 * representation.
 *
 * @author Michael Eichberg
 */
trait TACOptimization[P <: AnyRef, V <: Var[V]] {

    /**
     * Transforms the given code to the target code.
     */
    def apply(tac: TACOptimizationResult[P, V]): TACOptimizationResult[P, V]
}

/**
 * Encapsulates the result of an optimization/transformation of some three-address code.
 *
 * @author Michael Eichberg
 */
case class TACOptimizationResult[P <: AnyRef, V <: Var[V]](
        code:           TACode[P, V],
        wasTransformed: Boolean
)
