/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL
import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.LazyL3FieldAssignabilityAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Tobias Roth
 */
class ImmutabilityBenchmarkTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/cifi_benchmark/sandbox")
    }

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)
    }

    describe("all immutability analyses are executed") {

        val as = executeAnalyses(
            Set(
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL3FieldAssignabilityAnalysis,
                LazyL0FieldImmutabilityAnalysis,
                LazyL1ClassImmutabilityAnalysis,
                LazyL1TypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazySimpleEscapeAnalysis
            )
        )

        as.propertyStore.shutdown()

        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("ClassImmutability")
        )
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability")
        )
    }
}
