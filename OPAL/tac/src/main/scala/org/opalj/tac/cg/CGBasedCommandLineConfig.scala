/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.language.postfixOps

import org.rogach.scallop.ScallopConf

import org.opalj.ai.cli.AIBasedCommandLineConfig
import org.opalj.br.android.AndroidManifestArg
import org.opalj.br.fpcf.cli.cg.DisabledCGModulesArg
import org.opalj.br.fpcf.cli.cg.EnabledCGModulesArg
import org.opalj.br.fpcf.cli.cg.EntryPointsArg
import org.opalj.br.fpcf.cli.cg.MainClassArg
import org.opalj.br.fpcf.cli.cg.TamiFlexArg
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.log.OPALLogger
import org.opalj.si.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

trait CGBasedCommandLineConfig extends AIBasedCommandLineConfig with PropertyStoreBasedCommandLineConfig {
    self: ScallopConf =>

    private val cgArgGroup = group("Call-Graph related arguments:")

    private val cgArgs = Seq(
        CallGraphArg !,
        MainClassArg,
        EntryPointsArg,
        EnabledCGModulesArg,
        DisabledCGModulesArg,
        TamiFlexArg,
        AndroidManifestArg
    )

    args(cgArgs*)

    cgArgs.foreach { arg => argGroups += arg -> cgArgGroup }

    def setupCallGaph(project: Project): (CallGraph, Seconds) = {
        var callGraphTime = Seconds.None
        val callGraphKeyOpt = apply(CallGraphArg)
        if (callGraphKeyOpt.isEmpty) {
            (null, Seconds(0))
        } else {
            val callGraph = time {
                project.get(callGraphKeyOpt.get)
            } { t =>
                OPALLogger.info("analysis progress", s"setting up call graph took ${t.toSeconds} ")(
                    using project.logContext
                )
                callGraphTime = t.toSeconds
            }
            (callGraph, callGraphTime)
        }
    }
}
