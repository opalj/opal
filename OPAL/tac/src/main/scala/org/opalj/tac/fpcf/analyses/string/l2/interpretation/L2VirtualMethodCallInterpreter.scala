/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l2
package interpretation

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1VirtualMethodCallInterpreter
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Processes [[org.opalj.tac.VirtualMethodCall]]s without a call graph. Currently, only calls to `setLength` of string buffers and
 * string builders are interpreted. For other calls, ID is returned.
 *
 * @author Maximilian Rüsch
 */
case class L2VirtualMethodCallInterpreter()(
    implicit override val highSoundness: Boolean
) extends L1VirtualMethodCallInterpreter {

    override def interpretArbitraryCall(call: T)(implicit state: InterpretationState): ProperPropertyComputationResult = {
        computeFinalResult(StringFlowFunctionProperty.identity)
    }
}
