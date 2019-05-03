/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Enables specialized processing of subroutine calls by domains; this is generally only
 * relevant for those domains that record the control-flow graph.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
trait SubroutinesDomain {

    /**
     * @param pc The pc of the jsr(w) instruction.
     */
    def jumpToSubroutine(pc: Int, branchTarget: Int, returnTarget: Int): Unit = {}

    /**
     * @param pc The pc of the ret instruction.
     */
    def returnFromSubroutine(pc: Int, lvIndex: Int): Unit = {}

}
