/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package lcp_on_fields

import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.properties.lcp_on_fields.LCPOnFieldsProperty
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty
import org.opalj.ide.integration.LazyIDEAnalysisProxyScheduler
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis

/**
 * Test runner for linear constant propagation on fields.
 *
 * @author Robin Körkemeier
 */
class LCPOnFieldsTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/lcp_on_fields")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LCPOnFieldsAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisSchedulerExtended,
            LCPOnFieldsAnalysisScheduler,
            new LazyIDEAnalysisProxyScheduler(LinearConstantPropagationAnalysisSchedulerExtended),
            new LazyIDEAnalysisProxyScheduler(LCPOnFieldsAnalysisScheduler) {
                override def afterPhaseScheduling(propertyStore: PropertyStore, analysis: FPCFAnalysis): Unit = {
                    val entryPoints = methodsWithAnnotations(analysis.project)
                    entryPoints.foreach { case (method, _, _) =>
                        propertyStore.force(method, LCPOnFieldsAnalysisScheduler.propertyMetaInformation.key)
                        propertyStore.force(
                            method,
                            LinearConstantPropagationAnalysisSchedulerExtended.propertyMetaInformation.key
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
