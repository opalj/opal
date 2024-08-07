/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.linear_constant_propagation

import org.opalj.fpcf.ide.IDEPropertiesTest
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty

class LinearConstantPropagationTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/linear_constant_propagation")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LinearConstantPropagationAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisScheduler
        ))

        methodsWithAnnotations(testContext.project)
            .foreach { case (method, _, _) =>
                testContext.propertyStore.force(method, LinearConstantPropagationAnalysisScheduler.property.key)
            }

        testContext.propertyStore.waitOnPhaseCompletion()
        testContext.propertyStore.shutdown()

        validateProperties(
            testContext,
            methodsWithAnnotations(testContext.project),
            Set(LinearConstantPropagationProperty.KEY)
        )
    }
}
