/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.cli.AnalysisLevelCommand

object FieldAssignabilityCommand extends AnalysisLevelCommand(
        "Field-assignability analysis used.",
        "L0" -> "L0FieldAssignabilityAnalysis",
        "L1" -> "L1FieldAssignabilityAnalysis",
        "L2" -> "L2FieldAssignabilityAnalysis"
    ) {
    override val name: String = "fieldAssignability"
}
