/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigValueFactory.fromAnyRef
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.EagerL0FieldReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * Tests the field reference immutability analysis
 *
 * @author Tobias Peter Roth
 */
class FieldReferenceImmutabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability/sandbox")
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
        validateProperties(as, fieldsWithAnnotations(as.project), Set("ReferenceImmutability"))
    }

    L2PurityAnalysis_new.setRater(Some(SystemOutLoggingAllExceptionRater))

    describe("the org.opalj.fpcf.analyses.L0ReferenceImmutability is executed") {
        val as = executeAnalyses(
            Set(
                EagerL0FieldReferenceImmutabilityAnalysis,
                LazyL0FieldImmutabilityAnalysis,
                LazyLxClassImmutabilityAnalysis_new,
                LazyLxTypeImmutabilityAnalysis_new,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL2PurityAnalysis_new,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                LazyL1FieldMutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("ReferenceImmutability"))
    }
}
