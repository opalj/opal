/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package linear_constant_propagation

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty
import org.opalj.ide.integration.EagerIDEAnalysisProxyScheduler
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase

/**
 * Test runner for linear constant propagation.
 *
 * @author Robin Körkemeier
 */
class LinearConstantPropagationTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/linear_constant_propagation")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LinearConstantPropagationAnalysis") {
        val linearConstantPropagationAnalysisScheduler =
            new LinearConstantPropagationAnalysisScheduler() with JavaIDEAnalysisSchedulerBase.RTACallGraph

        val testContext = executeAnalyses(Set(
            linearConstantPropagationAnalysisScheduler,
            new EagerIDEAnalysisProxyScheduler(
                linearConstantPropagationAnalysisScheduler,
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
