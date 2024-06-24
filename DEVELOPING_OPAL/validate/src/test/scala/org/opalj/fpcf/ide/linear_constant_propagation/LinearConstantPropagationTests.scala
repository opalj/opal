/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.linear_constant_propagation

import java.net.URL
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.linear_constant_propagation.LinearConstantPropagationProperty
import org.opalj.ide.ConfigKeyDebugLog
import org.opalj.ide.ConfigKeyTraceLog
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationAnalysis

class LinearConstantPropagationTests extends PropertiesTest {
    override def withRT: Boolean = true

    override def createConfig(): Config = {
        super.createConfig()
            .withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix + "AllInstantiatedTypesFinder.projectClassesOnly",
                ConfigValueFactory.fromAnyRef(false)
            )
            .withValue(ConfigKeyDebugLog, ConfigValueFactory.fromAnyRef(true))
            .withValue(ConfigKeyTraceLog, ConfigValueFactory.fromAnyRef(false))
    }

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(_ =>
            Set[Class[? <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        )
        p.get(RTACallGraphKey)
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/linear_constant_propagation")
    }

    describe("Execute the o.o.t.f.a.i.l.LinearConstantPropagationAnalysis") {
        val testContext = executeAnalyses(Set(
            LinearConstantPropagationAnalysisScheduler
        ))

        testContext.analyses.foreach {
            case analysis: LinearConstantPropagationAnalysis =>
                analysis.lcpProblem.getEntryPoints
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
