/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ifds

import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.vta.ExpectedCallee
import org.opalj.fpcf.properties.vta.ExpectedType
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSFact
import org.opalj.tac.cg.RTACallGraphKey

/**
 * Tests the IFDS based variable type analysis
 *
 * @author Marc Clement
 */
class VTATest extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey
        )((_: Option[Set[Class[_ <: AnyRef]]]) =>
            Set[Class[_ <: AnyRef]](
                classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]]
            )
        )
        p.get(RTACallGraphKey)
    }

    describe("Executes the VTA and checks the resulting types and callees") {
        val testContext = executeAnalyses(new IFDSBasedVariableTypeAnalysisScheduler)
        val project = testContext.project
        val ps = testContext.propertyStore
        testContext.analyses.foreach {
            case ifdsAnalysis: IFDSAnalysis[_, _, _] =>
                for (e <- ifdsAnalysis.ifdsProblem.entryPoints) {
                    ps.force(e, ifdsAnalysis.propertyKey.key)
                }
            case _ => None
        }
        val eas = methodsWithAnnotations(project).map {
            case (method, entityString, annotations) =>
                ((method, new IFDSFact(VTANullFact)), entityString, annotations)
        }
        ps.shutdown()
        validateProperties(testContext, eas, Set(ExpectedType.PROPERTY_VALIDATOR_KEY))
        validateProperties(testContext, eas, Set(ExpectedCallee.PROPERTY_VALIDATOR_KEY))
    }

}
