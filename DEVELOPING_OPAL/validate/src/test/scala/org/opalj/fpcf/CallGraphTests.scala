/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.{InstantiatedTypesAnalysisScheduler, RTACallGraphAnalysisScheduler}

/**
 * Tests if the computed call graph contains (at least!) the expected call edges.
 *
 * @author Andreas Bauer
 */
class CallGraphTests extends PropertiesTest {

    describe("the RTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                InstantiatedTypesAnalysisScheduler,
                RTACallGraphAnalysisScheduler
            )
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees")
        )
    }

    describe("the CHA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(CHACallGraphAnalysisScheduler)
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees")
        )
    }
}
