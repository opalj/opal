/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.junit.runner.RunWith
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.fpcf.analyses.purity.EagerL1PurityAnalysis
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * Simple test to ensure that the [[org.opalj.fpcf.analyses.purity.L1PurityAnalysis]] does not cause
 * any exceptions.
 *
 * @author Dominik Helm
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class L1PuritySmokeTest extends FunSpec with Matchers {

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

    val baseAnalyses: Set[FPCFAnalysisScheduler] = Set(
        RTACallGraphAnalysisScheduler,
        TriggeredStaticInitializerAnalysis,
        TriggeredLoadedClassesAnalysis,
        TriggeredFinalizerAnalysisScheduler,
        TriggeredThreadRelatedCallsAnalysis,
        TriggeredSerializationRelatedCallsAnalysis,
        TriggeredReflectionRelatedCallsAnalysis,
        TriggeredInstantiatedTypesAnalysis,
        TriggeredConfiguredNativeMethodsAnalysis,
        TriggeredSystemPropertiesAnalysis,
        LazyCalleesAnalysis(
            Set(StandardInvokeCallees, SerializationRelatedCallees, ReflectionRelatedCallees)
        ),
        LazyL0BaseAIAnalysis,
        TACAITransformer
    )

    def checkProject(p: SomeProject, withSupportAnalyses: Boolean): Unit = {
        val manager = p.get(FPCFAnalysesManagerKey)

        manager.runAll(baseAnalyses)

        val analyses =
            if (withSupportAnalyses)
                primaryAnalyses ++ supportAnalyses
            else
                primaryAnalyses
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
