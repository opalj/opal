/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Adds support for handling return instructions in a generic manner.
 *
 * @author Michael Eichberg
 */
trait ReturnInstructionsDomain extends ai.ReturnInstructionsDomain {
    domain: ValuesDomain with Configuration with ExceptionsFactory =>

    protected[this] def handleReturn(pc: Int): Computation[Nothing, ExceptionValue] = {
        if (throwIllegalMonitorStateException) {
            val exception = VMIllegalMonitorStateException(pc)
            ComputationWithSideEffectOrException(exception)
        } else
            ComputationWithSideEffectOnly
    }

}
