/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

import org.opalj.fpcf.SomeEOptionP

/**
 * @author Maximilian Rüsch
 */
private[string_analysis] case class EPSDepender[T <: ASTNode[V], State <: ComputationState](
    instr:     T,
    pc:        Int,
    state:     State,
    dependees: Seq[SomeEOptionP]
) {

    def withDependees(newDependees: Seq[SomeEOptionP]): EPSDepender[T, State] = EPSDepender(
        instr,
        pc,
        state,
        newDependees
    )
}
