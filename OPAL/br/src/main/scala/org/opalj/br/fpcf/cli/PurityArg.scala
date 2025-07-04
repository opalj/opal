/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli

import org.opalj.cli.AnalysisLevelArg

object PurityArg extends AnalysisLevelArg(
        "Purity analysis used.",
        "L0" -> "L0PurityAnalysis",
        "L1" -> "L1PurityAnalysis",
        "L2" -> "L2PurityAnalysis"
    ) {
    override val name: String = "purity"
}
