/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package finalizer

/**
 * When processing instruction interprocedurally, it is not always possible to compute a final
 * result for an instruction. For example, consider the `append` operation of a StringBuilder where
 * the `append` argument is a call to another function. This function result is likely to be not
 * ready right away, which is why a final result for that `append` operation cannot yet be computed.
 * <p>
 * Implementations of this trait finalize the result for instructions. For instance, for `append`,
 * a finalizer would use all partial results (receiver and `append` value) to compute the final
 * result. However, '''this assumes that all partial results are available when finalizing a
 * result!'''
 */
trait L1Finalizer[State <: L1ComputationState[State]] {

    protected type T <: Any

    /**
     * Implementations of this class finalize an instruction of type [[T]] which they are supposed
     * to override / refine. This function does not return any result, however, the final result
     * computed in this function is to be set in [[ComputationState.fpe2sci]] at position `defSite` by concrete
     * implementations.
     *
     * @param instr The instruction that is to be finalized.
     * @param defSite The definition site that corresponds to the given instruction.
     */
    def finalizeInterpretation(instr: T, defSite: Int)(implicit state: State): Unit
}
