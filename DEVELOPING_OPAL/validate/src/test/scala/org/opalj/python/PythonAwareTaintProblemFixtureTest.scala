/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import org.opal.python.IFDSAnalysisPythonFixtureScheduler
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.taint.ForwardFlowPath
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintNullFact
import org.opalj.tac.cg.RTACallGraphKey

import java.net.URL

class PythonAwareTaintProblemFixtureTest extends PropertiesTest {
    override def fixtureProjectPackage: List[String] = List(
        "org/opalj/fpcf/fixtures/python"
    )

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey
        )(
            (_: Option[Set[Class[_ <: AnyRef]]]) =>
                Set[Class[_ <: AnyRef]](
                    classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]]
                )
        )
        p.get(RTACallGraphKey)
    }

    describe("Test the ForwardFlowPath annotations") {
        val testContext = executeAnalyses(IFDSAnalysisPythonFixtureScheduler)
        val project = testContext.project
        val eas = methodsWithAnnotations(project).map {
            case (method, entityString, annotations) =>
                ((method, TaintNullFact), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(ForwardFlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
