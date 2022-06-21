/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Records the program counters of all return (void) instructions that are reached.
 *
 * ==Usage==
 * Typical usage:
 * {{{
 * class MyDomain extends ...DefaultHandlingOfVoidReturns with RecordVoidReturns
 * }}}
 *
 * This domain forwards all instruction evaluation calls to the super trait.
 *
 * ==Core Properties==
 *  - Needs to be stacked upon a base implementation of the domain
 *    [[ReturnInstructionsDomain]].
 *  - Collects information directly associated with the analyzed code block.
 *  - Not thread-safe.
 *  - Not reusable.
 *
 * @author Michael Eichberg
 */
trait RecordVoidReturns extends ReturnInstructionsDomain {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    private[this] var returnVoidInstructions: PCs = NoPCs

    def allReturnVoidInstructions: PCs = returnVoidInstructions

    abstract override def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
        returnVoidInstructions += pc
        super.returnVoid(pc)
    }
}
