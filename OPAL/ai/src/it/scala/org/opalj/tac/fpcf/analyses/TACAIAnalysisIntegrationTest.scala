/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import java.util.concurrent.atomic.AtomicInteger

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import org.opalj.br.analyses.SomeProject
import org.opalj.br.TestSupport
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.concurrent.ConcurrentExceptions
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIResultAnalysis

/**
 * Tests that all methods of OPAL's test projects + the JDK can be converted to the ai-based
 * three address representation.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TACAIAnalysisIntegrationTest extends FunSpec with Matchers {

    def analyzeProject(
        theProject: SomeProject
    ): Unit = {
        if (Thread.currentThread().isInterrupted) return ;

        time { //  Test lazy analysis...
            val p = theProject.recreate()
            val counter = new AtomicInteger()
            val ps = p.get(PropertyStoreKey)
            ps.setupPhase(Set(BaseAIResult, TACAI), Set.empty)
            // LazyL0TACAIAnalysis.init(ps)
            // LazyL0TACAIAnalysis.schedule(ps, null)
            LazyL0BaseAIResultAnalysis.register(p,ps,null)
            TACAITransformer.init(ps)
            TACAITransformer.register(p,ps,null)
            try {
                p.parForeachMethodWithBody() { mi ⇒
                    counter.incrementAndGet() % 3 match {
                        case 0 ⇒
                            ps.force(mi.method, BaseAIResult.key)
                        case 1 ⇒
                            ps.force(mi.method, TACAI.key)
                        case 2 ⇒
                            ps.force(mi.method, BaseAIResult.key)
                            ps.force(mi.method, TACAI.key)
                    }
                }
            } catch {
                case t: ConcurrentExceptions ⇒
                    t.getSuppressed.foreach(_.printStackTrace())
                    throw t;
            }
            ps.waitOnPhaseCompletion()
            p.parForeachMethodWithBody() { mi ⇒
                val m = mi.method
                val aiResultProperty = ps(m, BaseAIResult.key)
                val Some(aiResult) = aiResultProperty.asFinal.p.aiResult
                // ... smoke test...
                val tacaiProperty = ps(m, TACAI.key)
                val Some(tac) = tacaiProperty.asFinal.p.tac
                // ... smoke test...
                counter.incrementAndGet()
            }

            ps.shutdown()
            info(s"lazily performed AI and generated TAC for ${counter.get / 2} method(s)")
        } { t ⇒ info(s"lazy analysis took ${t.toSeconds}") }

        time { // Test eager analysis...
            val p = theProject.recreate()
            val counter = new AtomicInteger()
            val fpcfManager = p.get(FPCFAnalysesManagerKey)
            val (ps,_/*executed analyses*/) = fpcfManager.runAll(EagerL0TACAIAnalysis)
            p.parForeachMethodWithBody() { mi ⇒
                val m = mi.method
                val aiResultProperty = ps(m, BaseAIResult.key)
                val Some(aiResult) = aiResultProperty.asFinal.p.aiResult
                // ... smoke test...
                val tacaiProperty = ps(m, TACAI.key)
                val Some(tac) = tacaiProperty.asFinal.p.tac
                // ... smoke test...
                counter.incrementAndGet()
            }
            ps.shutdown()
            info(s"eagerly performed AI and generated TAC for ${counter.get} method(s)")
        } { t ⇒ info(s"eager analysis took ${t.toSeconds}") }

    }

    // TESTS

    describe(s"creating the 3-address code using the PropertyStore") {

        //TestSupport.allManagedBITestProjects() foreach { biProject ⇒
        TestSupport.allBIProjects() foreach { biProject ⇒
            val (name, projectFactory) = biProject
            it(s"for $name") {
                analyzeProject(projectFactory())
            }
        }
    }
}
