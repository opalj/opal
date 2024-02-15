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
trait EPSDepender[T <: ASTNode[V], State <: ComputationState[State]] {
    type Self <: EPSDepender[T, State]

    def instr: T
    def pc: Int
    def state: State
    def dependees: Seq[SomeEOptionP]

    def withDependees(newDependees: Seq[SomeEOptionP]): Self
}

private[string_analysis] case class SimpleEPSDepender[T <: ASTNode[V], State <: ComputationState[State]](
    override val instr:     T,
    override val pc:        Int,
    override val state:     State,
    override val dependees: Seq[SomeEOptionP]
) extends EPSDepender[T, State] {

    type Self = SimpleEPSDepender[T, State]

    override def withDependees(newDependees: Seq[SomeEOptionP]): SimpleEPSDepender[T, State] = SimpleEPSDepender(
        instr,
        pc,
        state,
        newDependees
    )
}
