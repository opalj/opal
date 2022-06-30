/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerVirtualMethodPurityAnalysis
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.VirtualMethodPurity
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.purity.EagerL1PurityAnalysis

/**
 * Simple test to ensure that the [[org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis]] does not
 * cause any exceptions.
 *
 * @author Dominik Helm
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class L1PuritySmokeTest extends AnyFunSpec with Matchers {

    def reportAnalysisTime(t: Nanoseconds): Unit = { info(s"analysis took ${t.toSeconds}") }

    val primaryAnalyses: Set[ComputationSpecification[FPCFAnalysis]] = Set(
        EagerL1PurityAnalysis,
        EagerVirtualMethodPurityAnalysis
    )

    val supportAnalyses: Set[ComputationSpecification[FPCFAnalysis]] = Set(
        EagerL1FieldMutabilityAnalysis,
        EagerClassImmutabilityAnalysis,
        EagerTypeImmutabilityAnalysis
    )

    def checkProject(p: SomeProject, withSupportAnalyses: Boolean): Unit = {
        val manager = p.get(FPCFAnalysesManagerKey)

        p.get(RTACallGraphKey)

        var analyses = primaryAnalyses
        if (withSupportAnalyses)
            analyses ++= supportAnalyses
        manager.runAll(analyses)

        val propertyStore = p.get(PropertyStoreKey)
        try {
            if (propertyStore.entities(Purity.key).exists(_.isRefinable) ||
                propertyStore.entities(VirtualMethodPurity.key).exists(_.isRefinable)) {
                fail("Analysis left over non-final purity results")
            }
        } finally {
            propertyStore.shutdown()
        }
    }

    // TESTS

    describe("executing the L1 purity analysis should not fail") {

        allBIProjects() foreach { biProject =>
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
