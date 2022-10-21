/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds.old

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.{DeclaredMethodsKey, Project}
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.taint.BackwardFlowPath
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.properties.TaintNullFact
import org.opalj.tac.fpcf.analyses.ifds.taint.old.BackwardTaintAnalysisFixtureScheduler

import java.net.URL

/**
 * @author Mario Trageser
 */
class BackwardTaintAnalysisFixtureTest extends PropertiesTest {

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

    describe("Test the BackwardFlowPath annotations") {
        val testContext = executeAnalyses(BackwardTaintAnalysisFixtureScheduler)
        val project = testContext.project
        val declaredMethods = project.get(DeclaredMethodsKey)
        val eas = methodsWithAnnotations(project).map {
            case (methods, entityString, annotations) =>
                ((declaredMethods(methods), TaintNullFact), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(BackwardFlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
