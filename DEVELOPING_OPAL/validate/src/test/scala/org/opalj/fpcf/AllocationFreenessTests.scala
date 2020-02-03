/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.fpcf.analyses.EagerL0AllocationFreenessAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Dominik Helm
 */
class AllocationFreenessTests extends PropertiesTest {

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/allocation_freeness")
    }

    describe("the org.opalj.fpcf.analyses.L0AllocationFreenessAnalysis is executed") {
        val as = executeAnalyses(
            Set(EagerL0AllocationFreenessAnalysis)
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("AllocationFreeness")
        )
    }

}
