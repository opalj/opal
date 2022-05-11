/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class ClassAndTypeMutabilityTests extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.get(RTACallGraphKey)
    }

    describe("the field, class and type mutability analyses are executed") {
        val as = executeAnalyses(Set(
            EagerClassImmutabilityAnalysis,
            EagerTypeImmutabilityAnalysis,
            LazyUnsoundPrematurelyReadFieldsAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyL0BaseAIAnalysis
        ))
        as.propertyStore.shutdown()
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp => (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability", "ClassImmutability")
        )
    }

}
