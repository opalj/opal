/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import com.typesafe.config.Config

import org.opalj.cli.ConvertedCommand
import org.opalj.cli.ForwardingCommand
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.fpcf.par.SchedulingStrategyCommand
import org.opalj.si.Project

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.flagConverter

trait FPCFBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>
    def getScheduler(analysisName: String, eager: Boolean): FPCFAnalysisScheduler[_] = {
        if (eager) FPCFAnalysesRegistry.eagerFactory(analysisName)
        else FPCFAnalysesRegistry.lazyFactory(analysisName)
    }
}

object PropertyStoreDebugCommand extends ConvertedCommand[Boolean, Boolean]
    with ForwardingCommand[Boolean, Boolean, Boolean] {
    val command = org.opalj.cli.DebugCommand

    override def apply(config: Config, value: Option[Boolean]): Config = {
        PropertyStore.updateDebug(value.getOrElse(false))
        config
    }
}

trait PropertyStoreBasedCommandLineConfig extends FPCFBasedCommandLineConfig { self: ScallopConf =>
    generalCommands(PropertyStoreDebugCommand, SchedulingStrategyCommand)

    def setupPropertyStore(project: Project): PropertyStore = {
        project.get(PropertyStoreKey)
    }
}
