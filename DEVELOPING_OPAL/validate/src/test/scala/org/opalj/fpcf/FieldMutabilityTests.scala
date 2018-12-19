/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.analyses.EagerL0FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0TACAIAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Michael Eichberg
 */
class FieldMutabilityTests extends PropertiesTest {

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L0FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(EagerL0FieldMutabilityAnalysis)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

    describe("the org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis is executed") {
        val as = executeAnalyses(EagerL1FieldMutabilityAnalysis, LazyL0TACAIAnalysis)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
    }

}
