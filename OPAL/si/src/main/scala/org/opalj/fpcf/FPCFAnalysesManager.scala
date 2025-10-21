/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.debug
import org.opalj.si.Project
import org.opalj.util.PerformanceEvaluation.*

/**
 * Enables the execution of a set of analyses.
 *
 * To get an instance use the respective `FPCFAnalysesManagerKey`.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
class FPCFAnalysesManager private[fpcf] (val project: Project) {

    // caching (by means of using local fields) is not necessary
    private implicit final def logContext: LogContext = project.logContext
    private final def config: Config = project.config
    private final def propertyStore: PropertyStore = project.get(PropertyStoreKey)
    private final def trace: Boolean = config.getBoolean(FPCFAnalysesManager.TraceConfigKey)

    private var schedules: List[Schedule[FPCFAnalysis]] = Nil

    /**
     * Returns the executed schedules. The head is the latest executed schedule.
     */
    def executedSchedules: List[Schedule[FPCFAnalysis]] = schedules

    final def runAll[T <: FPCFAnalysis](
        analyses: ComputationSpecification[T]*
    ): (PropertyStore, List[(ComputationSpecification[FPCFAnalysis], FPCFAnalysis)]) = {
        runAll(analyses.to(Iterable))
    }

    final def runAll[T <: FPCFAnalysis](
        analyses:             Iterable[ComputationSpecification[T]],
        afterPhaseScheduling: List[ComputationSpecification[FPCFAnalysis]] => Unit = _ => ()
    ): (PropertyStore, List[(ComputationSpecification[FPCFAnalysis], FPCFAnalysis)]) = this.synchronized {

        val scenario =
            AnalysisScenario(analyses.asInstanceOf[Iterable[ComputationSpecification[FPCFAnalysis]]], propertyStore)

        val schedule = scenario.computeSchedule(propertyStore, FPCFAnalysesRegistry.defaultAnalysis)
        schedules ::= schedule

        if (trace) { debug("analysis progress", "executing " + schedule) }
        val as = time {
            schedule(propertyStore, trace, afterPhaseScheduling = afterPhaseScheduling)
        } { t =>
            if (trace) {
                debug("analysis progress", s"execution of schedule took ${t.toSeconds}")
            }
        }
        if (trace) {
            debug(
                "analysis progress",
                scenario.allProperties.map(p => PropertyKey.name(p.pk.id))
                    .mkString("used and derived properties = {", ", ", "}")
            )
        }
        (propertyStore, as)
    }
}

object FPCFAnalysesManager {

    final val TraceConfigKey = "org.opalj.fpcf.analyses.FPCFAnalysesManager.Trace"

}
