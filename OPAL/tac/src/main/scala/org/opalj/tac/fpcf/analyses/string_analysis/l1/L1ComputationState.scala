/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1

import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.string_analysis.l0.L0ComputationState

trait L1ComputationState extends L0ComputationState {

    val methodContext: Context
}
