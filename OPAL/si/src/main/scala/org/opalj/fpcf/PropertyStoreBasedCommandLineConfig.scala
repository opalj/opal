/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.cli.Command
import org.opalj.cli.ConvertedCommand
import org.opalj.cli.ForwardingCommand
import org.opalj.cli.OPALCommandLineConfig

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.flagConverter

trait FPCFBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>
    def getScheduler(analysisName: String, eager: Boolean): FPCFAnalysisScheduler[_] = {
        if (eager) FPCFAnalysesRegistry.eagerFactory(analysisName)
        else FPCFAnalysesRegistry.lazyFactory(analysisName)
    }
}

trait PropertyStoreBasedCommand[T, R] extends Command[T, R] {

    final def apply(cliConfig: OPALCommandLineConfig): Unit = {
        this(cliConfig.get(this))
    }

    def apply(value: Option[R]): Unit = {}
}

object PropertyStoreDebugCommand extends ConvertedCommand[Boolean, Boolean]
    with ForwardingCommand[Boolean, Boolean, Boolean] with PropertyStoreBasedCommand[Boolean, Boolean] {
    val command = org.opalj.cli.DebugCommand

    override def apply(value: Option[Boolean]): Unit = {
        PropertyStore.updateDebug(value.get)
    }
}

trait PropertyStoreBasedCommandLineConfig extends FPCFBasedCommandLineConfig { self: ScallopConf =>
    commands(PropertyStoreDebugCommand)

    def setupPropertyStore(): Unit = {
        commandsIterator.foreach {
            case command: PropertyStoreBasedCommand[_, _] => command(this)
            case _                                        =>
        }
    }
}
