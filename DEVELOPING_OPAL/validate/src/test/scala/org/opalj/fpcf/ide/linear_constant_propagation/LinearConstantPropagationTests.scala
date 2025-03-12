/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package linear_constant_propagation

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty
import org.opalj.ide.integration.EagerIDEAnalysisProxyScheduler

class LinearConstantPropagationTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/linear_constant_propagation")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LinearConstantPropagationAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisScheduler,
            new EagerIDEAnalysisProxyScheduler(
                LinearConstantPropagationAnalysisScheduler,
                { (project: SomeProject) => methodsWithAnnotations(project).map(_._1) }
            )
        ))

        testContext.propertyStore.shutdown()

        validateProperties(
            testContext,
            methodsWithAnnotations(testContext.project),
            Set(LinearConstantPropagationProperty.KEY)
        )
    }
}
