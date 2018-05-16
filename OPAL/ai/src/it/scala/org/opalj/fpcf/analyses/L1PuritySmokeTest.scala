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
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * Simple test to ensure that the [[L1PurityAnalysis]] does not cause any exceptions.
 *
 * @author Dominik Helm
 *         @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class L1PuritySmokeTest extends FunSpec with Matchers {

    def reportAnalysisTime(t : Nanoseconds ): Unit = { info(s"analysis took ${t.toSeconds}") }

    val primaryAnalyses : Set[ComputationSpecification] =Set(
        EagerL1PurityAnalysis,
        EagerVirtualMethodPurityAnalysis
    )

    val supportAnalyses : Set[ComputationSpecification] =        Set(
            EagerL1FieldMutabilityAnalysis,
            EagerClassImmutabilityAnalysis,
            EagerTypeImmutabilityAnalysis
        )

    def checkProject(p: SomeProject, withSupportAnalyses: Boolean): Unit = {
        val analyses  =
            if (withSupportAnalyses)
                primaryAnalyses ++ supportAnalyses
            else
                primaryAnalyses
        p.get(FPCFAnalysesManagerKey).runAll(analyses)

        val propertyStore = p.get(PropertyStoreKey)
        // TODO @Florian... the following test seems to be broken; the error report should be more telling when it fails
        if (!propertyStore.entities(Purity.key).exists(_.isRefinable) ||
            !propertyStore.entities(VirtualMethodPurity.key).exists(_.isRefinable)) {
            fail("Analysis left over non-final purity results")
        }
    }

    // TESTS

    describe ("executing the L1 purity analysis should not fail") {


        allBIProjects() foreach { biProject ⇒
            val (name, projectFactory) = biProject

            it(s"for $name when no support analyses are scheduled") {
                val p = projectFactory()
                time {
                    checkProject(p, withSupportAnalyses = false)
                }(reportAnalysisTime)
            }

            it(s"for $name when support analyses are scheduled") {
                val p = projectFactory()
                time {
                    checkProject(p, withSupportAnalyses = true)
                }(reportAnalysisTime)
            }
        }

        it("for the JDK without support analyses") {
            val p = createJREProject()
            time {
                checkProject(p, withSupportAnalyses = false)
            }(reportAnalysisTime)
        }

        it("for the JDK with support support analyses") {
            val p = createJREProject()
            time {
                checkProject(p, withSupportAnalyses = true)
            }(reportAnalysisTime)
        }
    }
}
