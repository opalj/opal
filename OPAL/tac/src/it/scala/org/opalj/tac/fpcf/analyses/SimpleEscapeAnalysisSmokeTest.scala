/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis

/**
 * Tests that the [[org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis]] does not
 * throw exceptions.
 *
 * @author Florian Kübler
 */
@RunWith(classOf[JUnitRunner])
class SimpleEscapeAnalysisSmokeTest extends AnyFunSpec with Matchers {

    def reportAnalysisTime(t: Nanoseconds): Unit = { info(s"analysis took ${t.toSeconds}") }

    def checkProject(p: SomeProject): Unit = {
        val ps = p.get(PropertyStoreKey)
        try {
            val manager = p.get(FPCFAnalysesManagerKey)
            manager.runAll(
                // LazyL0TACAIAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer,
                EagerSimpleEscapeAnalysis
            )
            ps.waitOnPhaseCompletion()
        } finally {
            ps.shutdown()
        }
    }

    describe(s"executing the simple escape analysis should not fail") {

        allBIProjects() foreach { biProject =>
            val (name, projectFactory) = biProject
            it(s"for $name") {
                val p = projectFactory()
                time { checkProject(p) } { reportAnalysisTime }
            }
        }

        it(s"for the JDK") {
            val p = createJREProject()
            time { checkProject(p) } { reportAnalysisTime }
        }
    }
}
