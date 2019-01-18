/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * An abstract interpreter that can be interrupted by calling the AI's `interrupt` method
 * or by calling the executing thread's interrupt method.
 *
 * @author Michael Eichberg
 */
class InterruptableAI[D <: Domain] extends AI[D] {

    @volatile private[this] var doInterrupt: Boolean = false

    override def isInterrupted = doInterrupt || Thread.currentThread().isInterrupted()

    /**
     * After a call of this method the abstract interpretation of the current method
     * will be terminated before the evaluation of the next instruction starts.
     *
     * This functionality is appropriately synchronized to ensure a timely interruption.
     */
    def interrupt(): Unit = { doInterrupt = true }

    def resetInterrupt(): Unit = { doInterrupt = false }

}
