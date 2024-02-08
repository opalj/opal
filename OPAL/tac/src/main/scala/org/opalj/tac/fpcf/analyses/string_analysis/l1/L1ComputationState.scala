/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 *
 * @param entity The entity for which the analysis was started with.
 */
case class L1ComputationState(
        override val dm:     DeclaredMethod,
        override val entity: SContext
) extends ComputationState[L1ComputationState] {

    /**
     * Callees information regarding the declared method that corresponds to the entity's method
     */
    var callees: Callees = _

    /**
     * Callers information regarding the declared method that corresponds to the entity's method
     */
    var callers: Callers = _
}
