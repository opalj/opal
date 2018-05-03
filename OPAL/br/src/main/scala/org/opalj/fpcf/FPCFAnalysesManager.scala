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

import scala.collection.mutable

import net.ceedubs.ficus.Ficus._

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.debug
import org.opalj.log.OPALLogger.error
import org.opalj.br.analyses.SomeProject

/**
 * @author Michael Reif
 * @author Michael Eichberg
 */
class FPCFAnalysesManager private[fpcf] (val project: SomeProject) {

    implicit def logContext: LogContext = project.logContext

    val propertyStore = project.get(PropertyStoreKey)
    //  private[this] def propertyStore = project.get(PropertyStoreKey)

    final val doDebug = {
        project.config.as[Option[Boolean]](FPCFAnalysesManager.ConfigKey).getOrElse(false)
    }

    // Accesses to this field have to be synchronized
    private[this] final val derivedProperties = mutable.Set.empty[Int]

    private[this] def registerProperties(
        analysisRunner: FPCFEagerAnalysisScheduler
    ): Unit = derivedProperties.synchronized {
        assert(
            !analysisRunner.derives.exists { pKind ⇒ derivedProperties.contains(pKind.id) },
            s"FPCFAnalysisManager: a property has already been derived ${analysisRunner.derives}"
        )
        derivedProperties ++= analysisRunner.derives.map(_.id)
    }

    final def runAll(analyses: FPCFEagerAnalysisScheduler*): Unit = runAll(analyses, true)

    final def runAll(
        analyses:         Traversable[FPCFEagerAnalysisScheduler],
        waitOnCompletion: Boolean                                 = true
    ): Unit = {
        analyses.foreach { run(_, false) }
        if (waitOnCompletion) {
            propertyStore.waitOnPhaseCompletion()
        }
    }

    def run(
        analysisRunner:   FPCFEagerAnalysisScheduler,
        waitOnCompletion: Boolean                    = true
    ): Unit = this.synchronized {
        if (!isDerived(analysisRunner.derives)) {
            if (doDebug) {
                debug("analysis configuration", s"scheduling the analysis ${analysisRunner.name}")
            }

            registerProperties(analysisRunner)
            analysisRunner.start(project, propertyStore)
            if (waitOnCompletion) {
                propertyStore.waitOnPhaseCompletion()
            }
        } else {
            val runner = analysisRunner.name
            error("analysis configuration", s"$runner is running/was executed for this project")
        }
    }

    def isDerived(pKind: PropertyKind): Boolean = derivedProperties.synchronized {
        derivedProperties contains pKind.id
    }

    def isDerived(pKinds: Set[PropertyKind]): Boolean = pKinds exists (pKind ⇒ isDerived(pKind))

}

object FPCFAnalysesManager {

    final val ConfigKey = "org.opalj.fcpf.analyses.manager.debug"
}
