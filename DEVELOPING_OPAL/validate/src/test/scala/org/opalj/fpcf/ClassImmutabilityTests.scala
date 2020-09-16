/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigValueFactory.fromAnyRef

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey

/**
 * Tests the class immutability analysis
 *
 * @author Tobias Peter Roth
 */
class ClassImmutabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability")
    }

    override def createConfig(): Config = {

        val configForEntryPoints = BaseConfig.withValue(
            InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.AllEntryPointsFinder")
        ).withValue(
                InitialEntryPointsKey.ConfigKeyPrefix+"AllEntryPointsFinder.projectMethodsOnly",
                ConfigValueFactory.fromAnyRef(true)
            )

        configForEntryPoints.withValue(
            InitialInstantiatedTypesKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.AllInstantiatedTypesFinder")
        ).withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix+
                    "AllInstantiatedTypesFinder.projectClassesOnly",
                ConfigValueFactory.fromAnyRef(true)
            )
            .withValue(
                "org.opalj.br.analyses.cg.ClosedPackagesKey",
                fromAnyRef("org.opalj.br.analyses.cg.OpenCodeBase")
            )
            .withValue("org.opalj.br.analyses.cg.ClassExtensibilityKey", ConfigValueFactory.fromAnyRef(
                "org.opalj.br.analyses.cg.ConfiguredExtensibleClasses"
            ))
    }

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("ClassImmutability_new"))
    }

    describe("the org.opalj.fpcf.analyses.LxClassImmutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis_new,
                LazyL0FieldReferenceImmutabilityAnalysis,
                LazyL0FieldImmutabilityAnalysis,
                LazyLxTypeImmutabilityAnalysis_new,
                EagerLxClassImmutabilityAnalysis_new,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("ClassImmutability_new")
        )
    }
}
