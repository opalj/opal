/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.Code
import org.opalj.collection.immutable.IntTrieSet

/**
 * Tracks if a monitor(enter|exit) instruction was executed.
 *
 * This knowledge is of interest to decide, e.g., whether a return instruction
 * may throw an `IllegalMonitorStateException` or not.
 *
 * @author Michael Eichberg
 */
trait MonitorInstructionsTracker extends MonitorInstructionsDomain with CustomInitialization {
    this: ValuesDomain with ExceptionsFactory with Configuration =>

    protected[this] var usesMonitorInstruction: Boolean = _

    def isMonitorInstructionUsed: Boolean = usesMonitorInstruction

    abstract override def initProperties(
        code:          Code,
        cfJoins:       IntTrieSet,
        initialLocals: Locals
    ): Unit = {
        super.initProperties(code, cfJoins, initialLocals)

        this.usesMonitorInstruction = false
    }

    abstract override def monitorenter(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValue] = {
        usesMonitorInstruction = true

        super.monitorenter(pc, value)
    }

    abstract override def monitorexit(
        pc:    Int,
        value: DomainValue
    ): Computation[Nothing, ExceptionValues] = {
        usesMonitorInstruction = true

        super.monitorexit(pc, value)
    }

}

