/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0TACAIAnalysis
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time

/**
 * Tests that the [[EagerSimpleEscapeAnalysis]] does not throw exceptions.
 *
 * @author Florian Kübler
 */
@RunWith(classOf[JUnitRunner])
class SimpleEscapeAnalysisSmokeTest extends FunSpec with Matchers {

    def reportAnalysisTime(t: Nanoseconds): Unit = { info(s"analysis took ${t.toSeconds}") }

    def checkProject(p: SomeProject): Unit = {
        val ps = p.get(PropertyStoreKey)
        try {
            val manager = p.get(FPCFAnalysesManagerKey)
            manager.runAll(EagerSimpleEscapeAnalysis, LazyL0TACAIAnalysis)
            ps.waitOnPhaseCompletion()
        } finally {
            ps.shutdown()
        }
    }

    describe(s"executing the simple escape analysis should not fail") {

        allBIProjects() foreach { biProject ⇒
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
