/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Domain that defines all methods related to monitor instructions.
 *
 * @author Michael Eichberg
 */
trait MonitorInstructionsDomain { this: ValuesDomain =>

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note A monitor enter instruction may throw a `NullPointerException`.
     */
    def monitorenter(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * Handles a `monitorexit` instruction.
     *
     * @note A monitor exit instruction may throw a `NullPointerException` or an
     *      `IllegalMonitorStateException`.
     */
    def monitorexit(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValues]

}
