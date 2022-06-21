/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

/**
 * @author Michael Eichberg
 */
trait ReturnInstructionsDomain extends ai.ReturnInstructionsDomain with MonitorInstructionsTracker {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    /**
     * Creates a computation object that encapsulates the result of a computation that
     * may throw an `IllegalMonitorStateException` if a monitor is (potentially) used.
     * The primary example are the `(XXX)return` instructions.
     *
     * @param pc The program counter of a return instruction.
     */
    protected[this] def handleReturn(pc: Int): Computation[Nothing, ExceptionValue] = {
        if (isMonitorInstructionUsed && throwIllegalMonitorStateException) {
            val exception = IllegalMonitorStateException(ValueOriginForImmediateVMException(pc))
            ComputationWithSideEffectOrException(exception)
        } else {
            ComputationWithSideEffectOnly
        }
    }

}
