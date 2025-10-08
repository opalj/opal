/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.flagConverter
import org.rogach.scallop.intConverter

import org.opalj.cli.Arg
import org.opalj.cli.ConvertedArg
import org.opalj.cli.ForwardingArg
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.cli.ParsedArg
import org.opalj.cli.PlainArg
import org.opalj.fpcf.par.SchedulingStrategyArg
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.si.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

trait PropertyStoreBasedArg[T, R] extends Arg[T, R] {

    final def apply(project: Project, cliConfig: OPALCommandLineConfig): Unit = {
        apply(project, cliConfig.get(this))
    }

    def apply(project: Project, value: Option[R]): Unit = {}
}

object NoPropertyStoreArg extends PlainArg[Boolean] {
    override val name = "noPropertyStore"
    override val description = "Do not use the property store"
}

object PropertyStoreDebugArg extends ConvertedArg[Boolean, Boolean] with ForwardingArg[Boolean, Boolean, Boolean] {
    val arg = org.opalj.cli.DebugArg

    override def apply(config: Config, value: Option[Boolean]): Config = {
        PropertyStore.updateDebug(value.getOrElse(false))
        config
    }
}

object PropertyStoreThreadsNumArg extends ConvertedArg[Int, Int] with ForwardingArg[Int, Int, Int]
    with PropertyStoreBasedArg[Int, Int] {
    val arg = org.opalj.cli.ThreadsNumArg

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

trait PropertyStoreBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>
    generalArgs(
        PropertyStoreThreadsNumArg,
        PropertyStoreDebugArg,
        SchedulingStrategyArg,
        DisableCleanupArg,
        KeepPropertyKeysArg,
        ClearPropertyKeysArg
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

    def getScheduler(analysisName: String, eager: Boolean): FPCFAnalysisScheduler[_] = {
        if (eager) FPCFAnalysesRegistry.eagerFactory(analysisName)
        else FPCFAnalysesRegistry.lazyFactory(analysisName)
    }
}

object DisableCleanupArg extends PlainArg[Boolean] {
    override val name: String = "disableCleanup"
    override def description: String = "Disable cleanup of the PropertyStore inbetween phases"
    override val defaultValue: Option[Boolean] = Some(false)
    override def apply(config: Config, value: Option[Boolean]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.DisableCleanup",
            ConfigValueFactory.fromAnyRef(value.getOrElse(false))
        )
    }
}

object KeepPropertyKeysArg extends ParsedArg[List[String], List[SomePropertyKey]] {
    override val name: String = "keepPropertyKeys"
    override val description: String = "List of Properties to keep at the end of the analysis"
    override val defaultValue: Option[List[String]] = None

    override def apply(config: Config, value: Option[List[SomePropertyKey]]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.KeepPropertyKeys",
            ConfigValueFactory.fromAnyRef(value.getOrElse(""))
        )
    }

    override def parse(arg: List[String]): List[SomePropertyKey] = {
        arg.flatMap(_.split(",")).map(PropertyKey.getByName)
    }
}

object ClearPropertyKeysArg extends ParsedArg[List[String], List[SomePropertyKey]] {
    override val name: String = "clearPropertyKeys"
    override val description: String = "List of Properties to keep at the end of the analysis"
    override val defaultValue: Option[List[String]] = None

    override def apply(config: Config, value: Option[List[SomePropertyKey]]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.ClearPropertyKeys",
            ConfigValueFactory.fromAnyRef(value.getOrElse(""))
        )
    }

    override def parse(arg: List[String]): List[SomePropertyKey] = {
        arg.flatMap(_.split(",")).map(PropertyKey.getByName)
    }
}
