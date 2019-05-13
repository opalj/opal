/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
import java.net.URL

import org.opalj.fpcf.properties.taint.FlowPath
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.fpcf.analyses.NullFact
import org.opalj.tac.fpcf.analyses.TaintAnalysisFixture

/**
 * @author Mario Trageser
 */
class TaintAnalysisFixtureTest extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey
        )(
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        )
    }

    describe("Test the FlowPath annotations") {
        val testContext = executeAnalyses(TaintAnalysisFixture)
        val project = testContext.project
        val declaredMethods = project.get(DeclaredMethodsKey)
        val eas = methodsWithAnnotations(project).map {
            case (methods, entityString, annotations) ⇒ ((declaredMethods(methods), NullFact), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(FlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
