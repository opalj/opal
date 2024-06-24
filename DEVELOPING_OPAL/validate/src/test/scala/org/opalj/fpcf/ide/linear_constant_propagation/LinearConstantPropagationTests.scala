/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.linear_constant_propagation

import org.opalj.fpcf.ide.IDEPropertiesTest
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationAnalysis

class LinearConstantPropagationTests extends IDEPropertiesTest {
    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/linear_constant_propagation")
    }

    describe("Execute the o.o.t.f.a.i.i.l.LinearConstantPropagationAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisScheduler
        ))

        testContext.analyses.foreach {
            case analysis: LinearConstantPropagationAnalysis =>
                getEntryPointsByICFG(analysis.lcpProblem.icfg, testContext.project)
                    .foreach { method => testContext.propertyStore.force(method, analysis.propertyMetaInformation.key) }
        }

        testContext.propertyStore.shutdown()

        validateProperties(
            testContext,
            methodsWithAnnotations(testContext.project),
            Set(LinearConstantPropagationProperty.KEY)
        )
    }
}
