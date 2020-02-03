/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.fpcf.analyses.EagerL0CompileTimeConstancyAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Dominik Helm
 */
class CompileTimeConstancyTests extends PropertiesTest {

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/compile_time_constancy")
    }

    describe("the org.opalj.fpcf.analyses.L0CompileTimeConstancyAnalysis is executed") {
        val as = executeAnalyses(
            Set(EagerL0CompileTimeConstancyAnalysis)
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("CompileTimeConstancy"))
    }

}
