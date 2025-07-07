/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import com.typesafe.config.Config

import org.opalj.cli.Arg
import org.opalj.cli.ConvertedArg
import org.opalj.cli.ForwardingArg
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.fpcf.par.SchedulingStrategyArg
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.si.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.flagConverter
import org.rogach.scallop.intConverter

trait FPCFBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>
    def getScheduler(analysisName: String, eager: Boolean): FPCFAnalysisScheduler[_] = {
        if (eager) FPCFAnalysesRegistry.eagerFactory(analysisName)
        else FPCFAnalysesRegistry.lazyFactory(analysisName)
    }
}

trait PropertyStoreBasedArg[T, R] extends Arg[T, R] {

    final def apply(project: Project, cliConfig: OPALCommandLineConfig): Unit = {
        apply(project, cliConfig.get(this))
    }

    def apply(project: Project, value: Option[R]): Unit = {}
}

object PropertyStoreDebugArg extends ConvertedArg[Boolean, Boolean] with ForwardingArg[Boolean, Boolean, Boolean] {
    val command = org.opalj.cli.DebugArg

    override def apply(config: Config, value: Option[Boolean]): Config = {
        PropertyStore.updateDebug(value.getOrElse(false))
        config
    }
}

object PropertyStoreThreadsNumArg extends ConvertedArg[Int, Int] with ForwardingArg[Int, Int, Int]
    with PropertyStoreBasedArg[Int, Int] {
    val command = org.opalj.cli.ThreadsNumArg

    override def apply(project: Project, value: Option[Int]): Unit = {
        val numThreads = value.get
        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                implicit val lg: LogContext = project.logContext
                if (numThreads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
                    org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = numThreads
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )
    }
}

trait PropertyStoreBasedCommandLineConfig extends FPCFBasedCommandLineConfig { self: ScallopConf =>
    generalArgs(
        PropertyStoreThreadsNumArg,
        PropertyStoreDebugArg,
        SchedulingStrategyArg
    )

    def setupPropertyStore(project: Project): (PropertyStore, Seconds) = {
        var propertyStoreTime = Seconds.None
        val propertyStore = time {
            argsIterator.foreach {
                case arg: PropertyStoreBasedArg[_, _] => arg(project, this)
                case _                                =>
            }
            project.get(PropertyStoreKey)
        } { t =>
            OPALLogger.info("analysis progress", s"setting up property store took ${t.toSeconds} ")(project.logContext)
            propertyStoreTime = t.toSeconds
        }
        (propertyStore, propertyStoreTime)
    }
}
