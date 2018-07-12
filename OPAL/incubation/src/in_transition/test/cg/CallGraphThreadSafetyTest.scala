/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br._
import org.opalj.br.reader.Java8Framework

/**
 * Tests if the same same call graph is created when we run the call graph
 * algorithm multiple times. Given that the call graph algorithm is heavily
 * parallelized, this tests if there are no trivial concurrency bugs.
 *
 * @author Michael Eichberg
 */
class CallGraphThreadSafetyTest extends FlatSpec with Matchers {

    behavior of "OPAL's parallelized Call Graph algorithms"

    val CallGraphInstances = 50

    //
    // PROJECT SETUP
    //
    def testFileName = "classfiles/callgraph.jar"
    def testFilePath = "ai"
    def testFile = locateTestResources(testFileName, testFilePath)
    val classFiles = Java8Framework.ClassFiles(testFile)
    val project = br.analyses.Project(classFiles, Traversable.empty, true)
    def testCallGraphAlgorithm = new CHACallGraphAlgorithmConfiguration(project)

    //
    // GRAPH CONSTRUCTION
    //
    val callGraphs =
        (0 until CallGraphInstances).par map { i ⇒
            CallGraphFactory.create(
                project,
                () ⇒ CallGraphFactory.defaultEntryPointsForLibraries(project),
                testCallGraphAlgorithm
            )
        }

    //
    // TESTS
    //

    // Validate every method against the callgraph defined by annotations
    it should "always calculate the same call graph" in {
        //        println(callGraphs.head._1.callsStatistics(50))
        //        println(callGraphs.head._1.calledByStatistics(50))

        // some prerequisites to make sure the test makes sense...
        // the number of CallGraphs is as expected
        callGraphs.size should be(CallGraphInstances)
        // the CallGraphs are represented using different Objects
        callGraphs.seq.sliding(2).foreach(w ⇒ { val Seq(a, b) = w; a should not be (b) })

        // the actual comparison
        callGraphs.seq.reduce[ComputedCallGraph] { (l, r) ⇒
            val ComputedCallGraph(lcallGraph, lunresolvedMethodCalls, lexceptions) = l
            val ComputedCallGraph(rcallGraph, runresolvedMethodCalls, rexceptions) = r

            lcallGraph.calledByCount should be(rcallGraph.calledByCount)
            lcallGraph.callsCount should be(rcallGraph.callsCount)

            var lcalledBySet: Set[(Method, scala.collection.Map[Method, PCs])] = Set.empty
            lcallGraph.foreachCalledByMethod((method, callers) ⇒ lcalledBySet += ((method, callers)))
            var rcalledBySet: Set[(Method, scala.collection.Map[Method, PCs])] = Set.empty
            rcallGraph.foreachCalledByMethod((method, callers) ⇒ rcalledBySet += ((method, callers)))
            lcalledBySet should equal(rcalledBySet)

            var lcallsSet: Set[(Method, scala.collection.Map[PC, Iterable[Method]])] = Set.empty
            lcallGraph.foreachCallingMethod((method, callees) ⇒ lcallsSet += ((method, callees)))
            var rcallsSet: Set[(Method, scala.collection.Map[PC, Iterable[Method]])] = Set.empty
            rcallGraph.foreachCallingMethod((method, callees) ⇒ rcallsSet += ((method, callees)))

            lunresolvedMethodCalls.toSet should equal(runresolvedMethodCalls.toSet)
            lexceptions.toSet should equal(rexceptions.toSet)

            l
        }

    }

}
