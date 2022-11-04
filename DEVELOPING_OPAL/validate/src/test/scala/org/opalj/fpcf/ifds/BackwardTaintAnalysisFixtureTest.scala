/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.taint.BackwardFlowPath
import org.opalj.ifds.IFDSFact
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.taint.{BackwardTaintAnalysisFixtureScheduler, TaintNullFact}

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
        val eas = methodsWithAnnotations(project).map {
            case (method, entityString, annotations) =>
                ((method, new IFDSFact(TaintNullFact)), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(BackwardFlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
