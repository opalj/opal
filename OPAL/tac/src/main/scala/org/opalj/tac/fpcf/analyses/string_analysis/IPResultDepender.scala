/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

/**
 * @author Maximilian Rüsch
 */
trait IPResultDepender[T <: ASTNode[V], State <: ComputationState[State]] {
    type Self <: IPResultDepender[T, State]

    def instr: T
    def pc: Int
    def state: State
    def dependees: Seq[IPResult]

    def withDependees(newDependees: Seq[IPResult]): Self
}

private[string_analysis] case class SimpleIPResultDepender[T <: ASTNode[V], State <: ComputationState[State]](
    override val instr:     T,
    override val pc:        Int,
    override val state:     State,
    override val dependees: Seq[IPResult]
) extends IPResultDepender[T, State] {

    type Self = SimpleIPResultDepender[T, State]

    override def withDependees(newDependees: Seq[IPResult]): SimpleIPResultDepender[T, State] = SimpleIPResultDepender(
        instr,
        pc,
        state,
        newDependees
    )
}
