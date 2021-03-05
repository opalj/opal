/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds

import java.net.URL

import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.vta.ExpectedCallee
import org.opalj.fpcf.properties.vta.ExpectedType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.IFDSBasedVariableTypeAnalysis
import org.opalj.tac.fpcf.analyses.ifds.VTANullFact

class VTATest extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey
        )(
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set[Class[_ <: AnyRef]](
                    classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]]
                )
        )
        p.get(RTACallGraphKey)
    }

    describe("Test the ExpectedType annotations") {
        val testContext = executeAnalyses(IFDSBasedVariableTypeAnalysis)
        val project = testContext.project
        val declaredMethods = project.get(DeclaredMethodsKey)
        val eas = methodsWithAnnotations(project).map {
            case (methods, entityString, annotations) ⇒
                ((declaredMethods(methods), VTANullFact), entityString, annotations)
        }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(ExpectedType.PROPERTY_VALIDATOR_KEY))
        validateProperties(testContext, eas, Set(ExpectedCallee.PROPERTY_VALIDATOR_KEY))
    }

}
