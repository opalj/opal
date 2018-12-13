/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.debug
import org.opalj.log.OPALLogger.error
import org.opalj.util.PerformanceEvaluation._
import org.opalj.br.analyses.SomeProject

/**
 * Enables the execution of a set of analyses.
 *
 * To get an instance use the respective `FPCFAnalysesManagerKey`.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
class FPCFAnalysesManager private[fpcf] (val project: SomeProject) {

    // caching (by means of using local fields) is not necessary
    private[this] implicit final def logContext: LogContext = project.logContext
    private[this] final def config: Config = project.config
    private[this] final def propertyStore: PropertyStore = project.get(PropertyStoreKey)
    private[this] final def trace: Boolean = config.getBoolean(FPCFAnalysesManager.TraceConfigKey)

    // Accesses to the following fields have to be synchronized
    private[this] final val derivedProperties: Array[Boolean] = {
        new Array[Boolean](PropertyKind.SupportedPropertyKinds)
    }

    private[this] var schedules: List[Schedule] = Nil

    /**
     * Returns the executed schedules. The head is the latest executed schedule.
     */
    def executedSchedules: List[Schedule] = schedules

    final def runAll(analyses: ComputationSpecification*): PropertyStore = runAll(analyses.toSet)

    final def runAll(analyses: Set[ComputationSpecification]): PropertyStore = this.synchronized {
        val scenario = AnalysisScenario(analyses)
        val properties = scenario.allProperties
        if (properties exists { p ⇒
            if (derivedProperties(p.pk.id)) {
                error(
                    "analysis progress",
                    s"$p was computed in a previous run; no analyses were executed"
                )
                true // <=> previously executed
            } else {
                false // <=> not previously executed
            }
        }) {
            // ... some property (kind) was already computed/scheduled
            return propertyStore;
        }
        properties foreach { p ⇒ derivedProperties(p.pk.id) = true }

        val schedule = scenario.computeSchedule
        schedules ::= schedule

        if (trace) { debug("analysis progress", "executing "+schedule) }
        time {
            schedule(propertyStore, trace)
        } { t ⇒
            if (trace) debug("analysis progress", s"execution of schedule took ${t.toSeconds}")
        }
        if (trace) {
            debug(
                "analysis progress",
                properties.map(p ⇒ PropertyKey.name(p.pk.id)).mkString(
                    "used and derived properties = {", ", ", "}"
                )
            )
        }
        propertyStore
    }
}

object FPCFAnalysesManager {

    final val TraceConfigKey = "org.opalj.fpcf.analyses.FPCFAnalysesManager.Trace"

}
