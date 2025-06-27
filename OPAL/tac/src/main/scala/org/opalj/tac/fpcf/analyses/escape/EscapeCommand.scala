/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.cli.AnalysisLevelCommand

object EscapeCommand extends AnalysisLevelCommand(
        "Escape analysis used.",
        "L1" -> "SimpleEscapeAnalysis",
        "L2" -> "InterproceduralEscapeAnalysis"
    ) {
    override val name: String = "escape"
}
