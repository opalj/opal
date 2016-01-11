/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package analysis

import scala.collection.mutable
import net.ceedubs.ficus.Ficus._
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey

/**
 * @author Michael Reif
 * @author Michael Eichberg
 */
class FPCFAnalysesManager private[analysis] (val project: SomeProject) {

    val propertyStore = project.get(SourceElementsPropertyStoreKey)
    //  private[this] def propertyStore = project.get(SourceElementsPropertyStoreKey)

    final val debug = {
        project.config.as[Option[Boolean]](FPCFAnalysesManager.ConfigKey).getOrElse(false)
    }

    // Accesses to this field have to be synchronized
    private[this] final val derivedProperties = mutable.Set.empty[Int]

    private[this] def registerProperties(
        analysisRunner: FPCFAnalysisRunner
    ): Unit = derivedProperties.synchronized {
        assert(
            !analysisRunner.derivedProperties.exists { pKind ⇒ derivedProperties.contains(pKind.id) },
            s"FPCFAnalysisManager: a property has already been derived ${analysisRunner.derivedProperties}"
        )
        derivedProperties ++= analysisRunner.derivedProperties.map(_.id)
    }

    final def runAll(analyses: FPCFAnalysisRunner*): Unit = {
        runAll(analyses)(true)
    }

    final def runAll(
        analyses: Traversable[FPCFAnalysisRunner]
    )(
        waitOnCompletion: Boolean = true
    ): Unit = {
        analyses.foreach { run(_, false) }
        if (waitOnCompletion)
            propertyStore.waitOnPropertyComputationCompletion(
                useDefaultForIncomputableProperties = true
            )
    }

    def run(
        analysisRunner:   FPCFAnalysisRunner,
        waitOnCompletion: Boolean            = true
    ): Unit = this.synchronized {
        if (!isDerived(analysisRunner.derivedProperties)) {
            if (debug)
                OPALLogger.debug(
                    "project configuration",
                    s"the analysis ${analysisRunner.name} will be executed next"
                )(project.logContext)

            registerProperties(analysisRunner)
            analysisRunner.start(project, propertyStore)
            if (waitOnCompletion) {
                propertyStore.waitOnPropertyComputationCompletion(
                    useDefaultForIncomputableProperties = true
                )
            }
        } else {
            OPALLogger.error(
                "project configuration",
                s"the analysis ${analysisRunner.name} is running/was executed for this project"
            )(project.logContext)
        }
    }

    def runWithRecommended(
        runner: FPCFAnalysisRunner
    )(
        waitOnCompletion: Boolean = true
    ): Unit = {
        if (!isDerived(runner.derivedProperties)) {
            val analyses =
                (runner.recommendations ++ runner.requirements).
                    filterNot { ar ⇒ isDerived(ar.derivedProperties) }
            runAll(analyses)(false)
            run(runner, waitOnCompletion)
        }
    }

    def isDerived(pKind: PropertyKind): Boolean = derivedProperties.synchronized {
        derivedProperties contains pKind.id
    }

    def isDerived(pKinds: Set[PropertyKind]): Boolean = pKinds exists (pKind ⇒ isDerived(pKind))
}

object FPCFAnalysesManager {

    final val ConfigKey = "org.opalj.fcpf.analysis.manager.debug"
}
