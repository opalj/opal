/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cli

import org.opalj.cli.AnalysisLevelArg

object FieldAssignabilityArg extends AnalysisLevelArg(
        "Field-assignability analysis used.",
        "L0" -> "L0FieldAssignabilityAnalysis",
        "L1" -> "L1FieldAssignabilityAnalysis",
        "L2" -> "L2FieldAssignabilityAnalysis"
    ) {
    override val name: String = "fieldAssignability"
}
