/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli

import org.opalj.cli.AnalysisLevelArg

object EscapeArg extends AnalysisLevelArg(
        "Escape analysis used.",
        "L1" -> "SimpleEscapeAnalysis",
        "L2" -> "InterproceduralEscapeAnalysis"
    ) {
    override val name: String = "escape"
}
