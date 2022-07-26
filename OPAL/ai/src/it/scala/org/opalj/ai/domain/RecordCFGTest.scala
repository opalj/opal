/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import java.net.URL

import scala.collection.mutable
import scala.jdk.CollectionConverters._

import org.junit.runner.RunWith
import org.opalj.graphs.ControlDependencies
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.util.PerformanceEvaluation
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.Project
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.Method
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGFactory
import org.opalj.br.TestSupport.createJREProject

/**
 * Tests if we are able to compute the CFG as well as the dominator/post-dominator tree for
 * a larger number of classes.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RecordCFGTest extends AnyFunSpec with Matchers {

    private object DominatorsPerformanceEvaluation extends PerformanceEvaluation

    import DominatorsPerformanceEvaluation.{time => dTime}

    class RecordCFGDomain[I](val method: Method, val project: Project[URL])
        extends CorrelationalDomain
        with TheProject
        with TheMethod
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l1.DefaultReferenceValuesBinding
        with l1.NullPropertyRefinement
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelDynamicLoads
        with l0.TypeLevelLongValuesShiftOperators
        with RecordCFG // <=== the domain we are going to test!

    def terminateAfter[T >: Null <: AnyRef](millis: Long, msg: => String)(f: => T): T = {
        @volatile var result: T = null
        val t = new Thread(new Runnable { def run(): Unit = { result = f } })
        t.start()
        t.join(millis)
        t.interrupt()
        t.join(10)
        if (t.isAlive) {
            // this way it is no longer deprecated...
            t.getClass.getMethod("stop").invoke(t)
            throw new InterruptedException(s"$msg: didn't terminate in $millis milliseconds")
        }
        result
    }

    private def analyzeProject(name: String, project: Project[java.net.URL]): Unit = {
        info(s"the loaded project ($name) contains ${project.methodsCount} methods")

        val failures = new java.util.concurrent.ConcurrentLinkedQueue[(String, Throwable)]

        project.parForeachMethodWithBody() { methodInfo =>
            val method = methodInfo.method
            try {
                val domain = new RecordCFGDomain(method, project)
                val evaluatedInstructions = dTime(Symbol("AI")) {
                    BaseAI(method, domain).evaluatedInstructions
                }

                val bbBRCFG = dTime(Symbol("BasicBlocksBasedBRCFG")) {
                    CFGFactory(method.body.get, project.classHierarchy)
                }
                val bbAICFG = dTime(Symbol("BasicBlocksBasedAICFG")) { domain.bbCFG }

                val pcs = new mutable.BitSet(method.body.size)
                bbAICFG.allBBs.foreach { bbAI =>

                    assert(bbAI.startPC <= bbAI.endPC, s"${bbAI.startPC}> ${bbAI.endPC}")

                    if (!pcs.add(bbAI.startPC))
                        fail(
                            s"the (start) pc ${bbAI.startPC} "+
                                "was already used by some other basic block"
                        )
                    if (bbAI.endPC != bbAI.startPC) {
                        if (!pcs.add(bbAI.endPC))
                            fail(s"the bb's (end) pc ${bbAI.endPC} ($bbAI) "+
                                "was already used by some other basic block")
                    }

                    val bbBR = bbBRCFG.bb(bbAI.startPC)
                    if (bbBR.isStartOfSubroutine != bbAI.isStartOfSubroutine) {
                        fail(
                            s"inconsistent: bbBR.isStartOfSubroutine(${bbBR.isStartOfSubroutine})"+
                                s" and bbAI.isStartOfSubroutine (${bbAI.isStartOfSubroutine})"
                        )
                    }
                    val allBRPredecessors = bbBR.predecessors.collect { case bb: BasicBlock => bb }
                    val allAIPredecessors = bbAI.predecessors.collect { case bb: BasicBlock => bb }
                    allAIPredecessors.foreach { predecessorBB =>
                        if (!allBRPredecessors.exists { p => p.endPC == predecessorBB.endPC })
                            fail(
                                s"the aibb ($bbAI) has different predecessors than the brbb ($bbBR):"+
                                    allAIPredecessors.mkString("ai:{", ",", "} vs. ") +
                                    allBRPredecessors.mkString("br:{", ",", "}")
                            )
                    }
                }

                evaluatedInstructions.iterator.foreach { pc =>

                    domain.foreachSuccessorOf(pc) { succPC =>
                        domain.predecessorsOf(succPC).contains(pc) should be(true)
                    }

                    domain.foreachPredecessorOf(pc) { predPC =>
                        domain.allSuccessorsOf(predPC).contains(pc) should be(true)
                    }

                    val bb = bbAICFG.bb(pc)
                    if (bb eq null) {
                        fail(s"the evaluated instruction $pc is not associated with a basic block")
                    }
                    bb.startPC should be <= (pc)
                    bb.endPC should be >= (pc)
                }

                val dt = dTime(Symbol("Dominators")) { domain.dominatorTree }

                val postDT = dTime(Symbol("PostDominators")) { domain.postDominatorTree }

                val cdg =
                    terminateAfter[ControlDependencies](1000L, { method.toJava }) {
                        dTime(Symbol("ControlDependencies")) { domain.pdtBasedControlDependencies }
                    }

                evaluatedInstructions.iterator.foreach { pc =>
                    if (pc != dt.startNode &&
                        (dt.dom(pc) != dt.startNode) &&
                        !evaluatedInstructions.contains(dt.dom(pc))) {
                        fail(
                            s"the dominator instruction ${dt.dom(pc)} of instruction $pc "+
                                s"was not evaluated (dominator tree start node: ${dt.startNode}); "+
                                s"code size=${method.body.get.instructions.length}."
                        )
                    }
                    if (pc != postDT.startNode &&
                        postDT.dom(pc) != postDT.startNode &&
                        !evaluatedInstructions.contains(postDT.dom(pc))) {
                        fail(s"the post-dominator ${postDT.dom(pc)} of $pc was not evaluated")
                    }
                    try {
                        dTime(Symbol("QueryingControlDependencies")) {
                            cdg.xIsControlDependentOn(pc)(x => { /* "somke test" */ })
                        }
                    } catch {
                        case t: Throwable =>
                            t.printStackTrace
                            fail(s"getting the control dependency information for $pc failed", t)
                    }
                }

            } catch {
                case t: Throwable =>
                    t.printStackTrace()
                    failures.add((method.toJava, t))
            }
        }

        if (failures.size > 0) {
            val failureMessages = for { (failure, exception) <- failures.asScala } yield {
                var root: Throwable = exception
                while (root.getCause != null) root = root.getCause
                val location =
                    if (root.getStackTrace() != null && root.getStackTrace().length > 0) {
                        root.getStackTrace().take(5).map { stackTraceElement =>
                            stackTraceElement.getClassName+
                                " { "+
                                stackTraceElement.getMethodName+":"+stackTraceElement.getLineNumber+
                                " }"
                        }.mkString("; ")
                    } else {
                        "<location unavailable>"
                    }
                failure +
                    s"[${root.getClass.getSimpleName}: ${root.getMessage}; location: $location] "
            }

            val failuresHeader = s"${failures.size} exceptions occured in: \n"
            val failuresInfo = failureMessages.mkString(failuresHeader, "\n", "\n")
            fail(failuresInfo)
        }
    }

    describe("computing (post)dominator trees and control dependence information") {

        val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)

        def evaluateProject(projectName: String, projectFactory: () => Project[URL]): Unit = {
            it(s"should be possible for all methods of $projectName") {
                DominatorsPerformanceEvaluation.resetAll()
                val project = projectFactory()
                time {
                    analyzeProject(projectName, project)
                } { t => info("the analysis took (real time):                            "+t.toSeconds) }

                import DominatorsPerformanceEvaluation.getTime
                info("performing AI took (CPU time):                            "+getTime(Symbol("AI")).toSeconds)
                info("computing dominator information took (CPU time):          "+getTime(Symbol("Dominators")).toSeconds)

                val postDominatorsTime = getTime(Symbol("PostDominators")).toSeconds
                info("computing post-dominator information took (CPU time):     "+postDominatorsTime)

                val cdgTime = getTime(Symbol("ControlDependencies")).toSeconds
                info("computing control dependency information took (CPU time): "+cdgTime)
                val cdgQueryTime = getTime(Symbol("QueryingControlDependencies")).toSeconds
                info("querying control dependency information took (CPU time):  "+cdgQueryTime)

                val bbAICFGTime = getTime(Symbol("BasicBlocksBasedAICFG")).toSeconds
                info("constructing the AI based CFGs took (CPU time):           "+bbAICFGTime)

                val bbBRCFGTime = getTime(Symbol("BasicBlocksBasedBRCFG")).toSeconds
                info("constructing the BR based CFGs took (CPU time):           "+bbBRCFGTime)
            }
        }

        evaluateProject("the JDK", () => createJREProject())

        var projectsCount = 0
        br.TestSupport.allBIProjects(reader, None) foreach { biProject =>
            val (projectName, projectFactory) = biProject
            evaluateProject(projectName, projectFactory)
            projectsCount += 1
        }
        info(s"analyzed $projectsCount projects w.r.t. the correctness of the cfgs")

    }
}
