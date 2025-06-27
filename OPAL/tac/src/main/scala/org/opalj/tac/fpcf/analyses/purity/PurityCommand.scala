/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package purity

import org.opalj.cli.AnalysisLevelCommand

object PurityCommand extends AnalysisLevelCommand(
        "Purity analysis used.",
        "L0" -> "L0PurityAnalysis",
        "L1" -> "L1PurityAnalysis",
        "L2" -> "L2PurityAnalysis"
    ) {
    override val name: String = "purity"
}
