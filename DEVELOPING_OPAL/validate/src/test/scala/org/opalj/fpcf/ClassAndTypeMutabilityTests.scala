/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0TACAIAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class ClassAndTypeMutabilityTests extends PropertiesTest {

    describe("the field, class and type mutability analyses are executed") {
        val as = executeAnalyses(Set(
            EagerClassImmutabilityAnalysis, EagerTypeImmutabilityAnalysis
        ), Set(
            LazyL1FieldMutabilityAnalysis, LazyL0TACAIAnalysis
        ))
        as.propertyStore.shutdown()
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability", "ClassImmutability")
        )
    }

}
