/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

//import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import java.net.URL
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
/*import org.opalj.tac.fpcf.analyses.immutability.EagerL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL3FieldImmutabilityAnalysis */
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.LazyL0FieldReferenceImmutabilityAnalysis
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.tac.cg.RTACallGraphKey /*
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis */
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.br.fpcf.analyses.EagerL0ClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL3FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Tobias Roth
 * @author Florian Kuebler
 */
class ClassAndTypeImmutabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/benchmark/h_classAndTypeImmutability")
    }

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)
    }

    describe("the 0 class and type immutability analyses are executed") {

        val as = executeAnalyses(Set(
            EagerL0ClassImmutabilityAnalysis,
            EagerL0TypeImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2FieldImmutabilityAnalysis,
            LazyL0FieldReferenceImmutabilityAnalysis,
            LazyL0BaseAIAnalysis
        ))

        as.propertyStore.shutdown()

        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability", "ClassImmutability")
        )
    }

    describe("the 1 class and type immutability analysis are executed") {

        val as = executeAnalyses(Set(
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL2PurityAnalysis,
            LazyL0FieldReferenceImmutabilityAnalysis,
            LazyL3FieldImmutabilityAnalysis,
            EagerL1TypeImmutabilityAnalysis,
            EagerL1ClassImmutabilityAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyL0CompileTimeConstancyAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis
        ))

        as.propertyStore.shutdown()

        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability", "ClassImmutability")
        )
    }
}
