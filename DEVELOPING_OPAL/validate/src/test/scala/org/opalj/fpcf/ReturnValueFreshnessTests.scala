/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis

/**
 *  Tests if the return value freshness properties specified in the test project (the classes in the
 *  (sub-)package of org.opalj.fpcf.fixture) and the computed ones match. The actual matching is
 *  delegated to PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class ReturnValueFreshnessTests extends PropertiesTest {

    override def init(p: Project[URL]): Unit = {
        p.get(RTACallGraphKey)
    }

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/return_freshness")
    }

    val analysisSchedulers: Set[FPCFAnalysisScheduler] = Set[FPCFAnalysisScheduler](
        LazyInterProceduralEscapeAnalysis,
        LazyFieldLocalityAnalysis,
        EagerReturnValueFreshnessAnalysis
    )

    describe("return value freshness analysis is executed") {
        val as = executeAnalyses(analysisSchedulers)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            contextsWithAnnotations(as.project),
            Set("ReturnValueFreshness")
        )
    }
}
