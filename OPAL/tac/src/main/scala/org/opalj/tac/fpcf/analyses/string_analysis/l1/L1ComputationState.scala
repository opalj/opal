/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.analyses.string_analysis.l0.L0ComputationState

trait L1ComputationState[State <: L1ComputationState[State]] extends L0ComputationState[State] {

    val methodContext: Context

    /**
     * Callees information regarding the declared method that corresponds to the entity's method
     */
    var callees: Callees = _

    /**
     * Callers information regarding the declared method that corresponds to the entity's method
     */
    var callers: Callers = _
}
