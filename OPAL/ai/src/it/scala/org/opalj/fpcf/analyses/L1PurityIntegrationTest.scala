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
package analyses

import org.junit.runner.RunWith
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.util.PerformanceEvaluation.time
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * Simple integration test to ensure [[L1PurityAnalysis]] does not cause any exceptions.
 *
 * @author Dominik Helm
 */
@RunWith(classOf[JUnitRunner])
class L1PurityIntegrationTest extends FunSpec with Matchers {
    val dependencies =
        Seq(
            EagerL1FieldMutabilityAnalysis,
            EagerClassImmutabilityAnalysis,
            EagerTypeImmutabilityAnalysis
        )

    def checkProject(project: () ⇒ SomeProject, withDependencies: Boolean): Unit = {
        val p = project()
        val analysesManager = p.get(FPCFAnalysesManagerKey)
        val analyses =
            if (withDependencies)
                dependencies :+ EagerL1PurityAnalysis :+ EagerVirtualMethodPurityAnalysis
            else
                Seq(EagerL1PurityAnalysis, EagerVirtualMethodPurityAnalysis)
        analysesManager.runAll(analyses)

        val propertyStore = p.get(PropertyStoreKey)
        if (!propertyStore.entities(Purity.key).exists(_.isRefinable) ||
            !propertyStore.entities(VirtualMethodPurity.key).exists(_.isRefinable)) {
            val message = "Analysis left over non-final purity results"
            fail(message)
        }
    }

    // TESTS

    allBIProjects() foreach { biProject ⇒
        val (name, projectFactory) = biProject
        it(s"it should be able to run the analysis for $name without dependencies") {
            time {
                checkProject(projectFactory, withDependencies = false)
            } { t ⇒ info(s"analysis took ${t.toSeconds}") }
        }

        it(s"it should be able to run the analysis for $name with dependencies") {
            time {
                checkProject(projectFactory, withDependencies = true)
            } { t ⇒ info(s"analysis took ${t.toSeconds}") }
        }
    }

    it("it should be able to run the analysis for the JDK without dependencies") {
        time {
            checkProject(() ⇒ createJREProject(), withDependencies = false)
        } { t ⇒ info(s"analysis took ${t.toSeconds}") }
    }

    it("it should be able to run the analysis for the JDK with dependencies") {
        time {
            checkProject(() ⇒ createJREProject(), withDependencies = true)
        } { t ⇒ info(s"analysis took ${t.toSeconds}") }
    }
}
