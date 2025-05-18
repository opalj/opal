/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package linear_constant_propagation

import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.LinearConstantPropagationProperty
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.LCPOnFieldsProperty
import org.opalj.ide.integration.LazyIDEAnalysisProxyScheduler
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LinearConstantPropagationAnalysisSchedulerExtended
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase

/**
 * Test runner for linear constant propagation on fields.
 *
 * @author Robin Körkemeier
 */
class LCPOnFieldsTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/linear_constant_propagation/lcp_on_fields")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LCPOnFieldsAnalysis") {
        val linearConstantPropagationAnalysisSchedulerExtended =
            new LinearConstantPropagationAnalysisSchedulerExtended() with JavaIDEAnalysisSchedulerBase.RTACallGraph
        val lcpOnFieldsAnalysisScheduler =
            new LCPOnFieldsAnalysisScheduler() with JavaIDEAnalysisSchedulerBase.RTACallGraph

        val testContext = executeAnalyses(Set(
            linearConstantPropagationAnalysisSchedulerExtended,
            lcpOnFieldsAnalysisScheduler,
            new LazyIDEAnalysisProxyScheduler(linearConstantPropagationAnalysisSchedulerExtended),
            new LazyIDEAnalysisProxyScheduler(lcpOnFieldsAnalysisScheduler) {
                override def afterPhaseScheduling(propertyStore: PropertyStore, analysis: FPCFAnalysis): Unit = {
                    val entryPoints = methodsWithAnnotations(analysis.project)
                    entryPoints.foreach { case (method, _, _) =>
                        propertyStore.force(method, lcpOnFieldsAnalysisScheduler.propertyMetaInformation.key)
                        propertyStore.force(
                            method,
                            linearConstantPropagationAnalysisSchedulerExtended.propertyMetaInformation.key
                        )
                    }
                }
            },
            LazyL2FieldAssignabilityAnalysis,
            EagerFieldAccessInformationAnalysis
        ))

        testContext.propertyStore.shutdown()

        validateProperties(
            testContext,
            methodsWithAnnotations(testContext.project),
            Set(LCPOnFieldsProperty.KEY, LinearConstantPropagationProperty.KEY)
        )
    }
}
