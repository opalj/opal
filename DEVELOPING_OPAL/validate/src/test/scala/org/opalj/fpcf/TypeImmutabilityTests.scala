/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * Tests the Type Immutability Analysis with the new lattice
 *
 * @author Tobias Peter Roth
 */
class TypeImmutabilityTests extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }
        p.get(RTACallGraphKey)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("TypeImmutability_new"))
    }

    describe("the org.opalj.fpcf.analyses.LxTypeImmutabilityAnalysis_new is executed") {
        println(1)
        val as = executeAnalyses(
            Set(
                LazyTypeImmutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL0ReferenceImmutabilityAnalysis,
                LazyL2PurityAnalysis,
                LazyL2FieldMutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyLxClassImmutabilityAnalysis_new,
                EagerLxTypeImmutabilityAnalysis_new
            )
        )
        as.propertyStore.shutdown()
        val tmp = classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3))
        print("===========================>>>>>>>>>>>>>>>>>>>>>>"+tmp)
        validateProperties(
            as,
            tmp,
            // fieldsWithAnnotations(as.project),
            Set("TypeImmutability_new")
        ) //TODO class files ... with annotation
    }
}
