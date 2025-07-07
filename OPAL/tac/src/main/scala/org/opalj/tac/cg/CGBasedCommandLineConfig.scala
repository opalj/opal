/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.language.postfixOps

import org.opalj.ai.util.AIBasedCommandLineConfig
import org.opalj.br.fpcf.cli.cg.DisabledCGModulesArg
import org.opalj.br.fpcf.cli.cg.EnabledCGModulesArg
import org.opalj.br.fpcf.cli.cg.EntryPointsArg
import org.opalj.br.fpcf.cli.cg.MainClassArg
import org.opalj.br.fpcf.cli.cg.TamiFlexArg
import org.opalj.log.OPALLogger
import org.opalj.si.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import org.rogach.scallop.ScallopConf

trait CGBasedCommandLineConfig extends AIBasedCommandLineConfig { self: ScallopConf =>

    val cgArgGroup = group("Call-Graph related arguments:")

    val cgArgs = Seq(
        CallGraphArg !,
        MainClassArg,
        EntryPointsArg,
        EnabledCGModulesArg,
        DisabledCGModulesArg,
        TamiFlexArg
    )

    args(cgArgs *)

    cgArgs.foreach { arg => argGroups += arg -> cgArgGroup }

    def setupCallGaph(project: Project): (CallGraph, Seconds) = {
        var callGraphTime = Seconds.None
        val callGraph = time {
            project.get(get(CallGraphArg, RTACallGraphKey))
        } { t =>
            OPALLogger.info("analysis progress", s"setting up call graph took ${t.toSeconds} ")(project.logContext)
            callGraphTime = t.toSeconds
        }
        (callGraph, callGraphTime)
    }
}
