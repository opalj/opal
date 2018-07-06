/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.debug
import org.opalj.log.OPALLogger.error
import org.opalj.util.PerformanceEvaluation._
import org.opalj.br.analyses.SomeProject

/**
 * Manages the execution of a set of analyses.
 *
 * To get an instance use the respective `FPCFAnalysesManagerKey`.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
class FPCFAnalysesManager private[fpcf] (
        val project: SomeProject
) {

    // caching (by means of using local fields) is not necessary
    private[this] implicit final def logContext: LogContext = project.logContext
    private[this] final def config: Config = project.config
    private[this] final def propertyStore: PropertyStore = project.get(PropertyStoreKey)
    private[this] final def trace: Boolean = config.getBoolean(FPCFAnalysesManager.TraceConfigKey)

    // Accesses to this field have to be synchronized
    private[this] final val derivedProperties: Array[Boolean] = {
        new Array[Boolean](PropertyKind.SupportedPropertyKinds)
    }

    final def runAll(analyses: ComputationSpecification*): PropertyStore = runAll(analyses.toSet)

    final def runAll(analyses: Set[ComputationSpecification]): PropertyStore = this.synchronized {
        val scenario = AnalysisScenario(analyses)
        val properties = scenario.allProperties
        if (properties exists { p ⇒
            if (derivedProperties(p.id)) {
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
        properties foreach { p ⇒ derivedProperties(p.id) = true }

        val schedule = scenario.computeSchedule
        // TODO Add schedule to Manager to make it available.

        if (trace) { debug("analysis progress", "executing "+schedule) }
        time {
            schedule(propertyStore)
        } { t ⇒
            if (trace) debug("analysis progress", s"execution of schedule took ${t.toSeconds}")
        }
        if (trace) {
            debug(
                "analysis progress",
                properties.map(p ⇒ PropertyKey.name(p.id)).mkString(
                    "used and derived properties = {", ", ", "}"
                )
            )
        }
        propertyStore
    }
}

object FPCFAnalysesManager {

    final val TraceConfigKey = "org.opalj.debug.fcpf.analyses.FPCFAnalysesManager.trace"

}
