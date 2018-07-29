/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.fpcf.analyses.EagerStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Dominik Helm
 */
class StaticDataUsageTests extends PropertiesTest {

    describe("the org.opalj.fpcf.analyses.StaticDataUsageAnalysis is executed") {
        val as = executeAnalyses(
            Set(EagerStaticDataUsageAnalysis),
            Set(LazyL0CompileTimeConstancyAnalysis)
        )
        as.propertyStore.shutdown()
        validateProperties(as, declaredMethodsWithAnnotations(as.project), Set("StaticDataUsage"))
    }

}
