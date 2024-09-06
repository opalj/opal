/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.lcp_on_fields

import org.opalj.fpcf.ide.IDEPropertiesTest
import org.opalj.fpcf.properties.lcp_on_fields.LCPOnFieldsProperty
import org.opalj.ide.integration.LazyIDEAnalysisProxyScheduler

class LCPOnFieldsTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/lcp_on_fields")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LCPOnFieldsAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisSchedulerExtended,
            LCPOnFieldsAnalysisScheduler,
            new LazyIDEAnalysisProxyScheduler(LinearConstantPropagationAnalysisSchedulerExtended),
            new LazyIDEAnalysisProxyScheduler(LCPOnFieldsAnalysisScheduler)
        ))

        val entryPoints = methodsWithAnnotations(testContext.project)
        entryPoints.foreach { case (method, _, _) =>
            testContext.propertyStore.force(method, LCPOnFieldsAnalysisScheduler.propertyMetaInformation.key)
        }

        testContext.propertyStore.waitOnPhaseCompletion()
        testContext.propertyStore.shutdown()

        validateProperties(
            testContext,
            methodsWithAnnotations(testContext.project),
            Set(LCPOnFieldsProperty.KEY),
            failOnInterimResults = false
        )
    }
}
