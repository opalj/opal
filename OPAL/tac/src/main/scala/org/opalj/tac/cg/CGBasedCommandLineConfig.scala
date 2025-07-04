/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.fpcf.cli.cg.DisabledCGModulesArg
import org.opalj.br.fpcf.cli.cg.EnabledCGModulesArg
import org.opalj.br.fpcf.cli.cg.EntryPointsArg
import org.opalj.br.fpcf.cli.cg.MainClassArg
import org.opalj.br.fpcf.cli.cg.TamiFlexArg
import org.opalj.fpcf.FPCFBasedCommandLineConfig
import org.opalj.si.Project
import org.rogach.scallop.ScallopConf

import scala.language.postfixOps

trait CGBasedCommandLineConfig extends FPCFBasedCommandLineConfig { self: ScallopConf =>

    val cgArgGroup = group("Call-Graph related arguments:")

    val cgArgs = Seq(
        CallGraphArg !,
        MainClassArg,
        EntryPointsArg,
        EnabledCGModulesArg,
        DisabledCGModulesArg,
        TamiFlexArg,
        )

    args(cgArgs*)

    cgArgs.foreach { arg => argGroups += arg -> cgArgGroup }

    def setupCallGaph(project: Project): CallGraph = {
        project.get(get(CallGraphArg).getOrElse(RTACallGraphKey))
    }
}
