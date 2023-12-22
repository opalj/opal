/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1.DefaultReferenceValuesDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.ThreadRelatedCallsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

/**
 * Tests  ThreadRelatedCallsAnalysis
 *
 * @author Julius Naeumann
 */
class ThreadRelatedCallsTest extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(ContextProviderKey) {
            case Some(_) => throw new IllegalArgumentException()
            case None    => CHACallGraphKey.getTypeIterator(p)
        }
        val requiredDomains: Set[Class[_ <: AnyRef]] = Set(classOf[DefaultReferenceValuesDomainWithCFGAndDefUse[_]])
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => requiredDomains
            case Some(requirements) => requirements ++ requiredDomains
        }
    }

    override def createConfig(): Config = {
        val baseConfig = super.createConfig()
        // For these tests, we want to restrict entry points to "main" methods.
        // Also, no types should be instantiated by default.
        baseConfig.withValue(
            InitialEntryPointsKey.ConfigKeyPrefix + "analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
        ).withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix + "analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationInstantiatedTypesFinder")
            )
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/threads/")
    }

    describe("ThreadStartAnalysis is executed") {

        val as = executeAnalyses(
            Set(
                LazyTACAIProvider,
                EagerFieldAccessInformationAnalysis,
                AllocationSiteBasedPointsToAnalysisScheduler,
                CallGraphAnalysisScheduler,
                ThreadRelatedCallsAnalysisScheduler
            )
        )
        as.propertyStore.shutdown()

        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees", "VMReachable")
        )
    }

}
