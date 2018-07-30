/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import scala.Console.RED
import scala.Console.RESET
import scala.Console.err

import org.opalj.util.asMB
import org.opalj.util.PerformanceEvaluation.memory
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.io.writeAndOpen
import org.opalj.graphs.DefaultMutableNode
import org.opalj.graphs.toDot

import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.ai.PC
import org.opalj.ai.analyses.cg.CHACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.ai.analyses.cg.BasicVTACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.BasicVTAWithPreAnalysisCallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.DefaultVTACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.ExtVTACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.CFACallGraphAlgorithmConfiguration

/**
 * Visualizes call graphs using Graphviz.
 *
 * Given GraphViz's capabilities only small call graphs (10 to 20 nodes) can
 * be effectively visualized.
 *
 * @author Michael Eichberg
 */
object CallGraphVisualization {

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     *      or a directory containing the former. The second element must
     *      denote the beginning of a package name and will be used to filter
     *      the methods that are included in the call graph.
     */
    def main(args: Array[String]): Unit = {
        if ((args.size < 3) || (args.size > 4)) {
            println("You have to specify:")
            println("\t1) The algorithm to use (CHA, BasicVTA, DefaultVTA, ExtVTA, kCFA with k in {1,2,3,4,6}).")
            println("\t2) A jar/class file or a directory containing jar/class files.")
            println("\t3) A pattern that specifies which class/interface types should be included in the output.")
            println("\t4 - Optional) The number of seconds (max. 30) before the analysis starts (e.g., to attach a profiler).")
            sys.exit(-1)
        }

        // To make it possible to attach a profiler:
        if (args.size == 4) {
            try {
                val secs = Integer.parseInt(args(3), 10)
                if (secs > 30) {
                    err.println("\t4) The number of seconds before the analysis starts (max. 30).")
                    sys.exit(-30)
                }
                Thread.sleep(secs * 1000L)
            } catch {
                case _: NumberFormatException ⇒
                    err.println("\t4) The number of seconds before the analysis starts (max 30).")
                    sys.exit(-31)
            }
        }
        //

        //
        // PROJECT SETUP
        //
        val project =
            memory {
                //val ClassFileReader = new reader.Java8Framework
                time {
                    var cache = new BytecodeInstructionsCache
                    var ClassFileReader = new Java8FrameworkWithCaching(cache)
                    val fileName = args(1)
                    val file = new java.io.File(fileName)
                    if (!file.exists()) {
                        println(RED+"file does not exist: "+fileName + RESET)
                        sys.exit(-2)
                    }
                    val classFiles =
                        try {
                            ClassFileReader.ClassFiles(file)
                        } catch {
                            case e: Exception ⇒
                                println(RED+"cannot read file: "+e.getMessage + RESET)
                                sys.exit(-3)
                        }
                    cache = null
                    ClassFileReader = null
                    val project = Project(classFiles, Traversable.empty, true)
                    println(
                        project.statistics.map(e ⇒ "\t"+e._1+": "+e._2).toSeq.sorted.
                            mkString("Project statistics:\n\t", "\n\t", "")
                    )
                    project
                } { t ⇒ println("Setting up the project took "+t.toSeconds) }
            } { m ⇒ println("Required memory for base representation: "+asMB(m))+"\n" }

        val fqnFilter = args(2)

        //
        // GRAPH CONSTRUCTION
        //
        import CallGraphFactory.defaultEntryPointsForLibraries
        val callGraphAlgorithm = args(0)
        val ComputedCallGraph(callGraph, unresolvedMethodCalls, exceptions) =
            memory {
                val computedCallGraph = time {
                    val callGraphAlgorithmConfig = callGraphAlgorithm match {
                        case "CHA" ⇒
                            new CHACallGraphAlgorithmConfiguration(project)
                        case "BasicVTA" ⇒
                            new BasicVTACallGraphAlgorithmConfiguration(project)
                        case "BasicVTAWithPreAnalysis" ⇒
                            new BasicVTAWithPreAnalysisCallGraphAlgorithmConfiguration(project)
                        case "VTA" | "DefaultVTA" ⇒
                            new DefaultVTACallGraphAlgorithmConfiguration(project)
                        case "ExtVTA" ⇒
                            new ExtVTACallGraphAlgorithmConfiguration(project)
                        case "1CFA" ⇒
                            new CFACallGraphAlgorithmConfiguration(project, 1)
                        case "2CFA" ⇒
                            new CFACallGraphAlgorithmConfiguration(project, 2)
                        case "CFA" | "3CFA" ⇒
                            new CFACallGraphAlgorithmConfiguration(project, 3)
                        case "4CFA" ⇒
                            new CFACallGraphAlgorithmConfiguration(project, 4)
                        case "6CFA" ⇒
                            new CFACallGraphAlgorithmConfiguration(project, 6)
                        case cga ⇒
                            println("Unknown call graph algorithm: "+cga+"; available: CHA, BasicVTA, DefaultVTA, ExtVTA")
                            return ;
                    }
                    val entryPoints = () ⇒ defaultEntryPointsForLibraries(project)
                    CallGraphFactory.create(project, entryPoints, callGraphAlgorithmConfig)
                } { t ⇒ println("Creating the call graph took: "+t) }

                // Some statistics
                val callGraph = computedCallGraph.callGraph
                import callGraph.calledByCount
                import callGraph.callsCount
                import callGraph.foreachCallingMethod
                println("Methods with at least one resolved call: "+callsCount)
                println("Methods which are called by at least one method: "+calledByCount)

                var maxCallSitesPerMethod = 0
                var methodWithMaxCallSites: Method = null
                var maxCallTargets = 0
                var methodWithMethodCallWithMaxTargets: Method = null
                var methodCallWithMaxTargetsPC: PC = 0
                foreachCallingMethod { (method, callees) ⇒
                    val callSitesCount = callees.size
                    if (callSitesCount > maxCallSitesPerMethod) {
                        maxCallSitesPerMethod = callSitesCount
                        methodWithMaxCallSites = method
                    }
                    val maxTargetsPerCallSite =
                        callees.values.map(_.size).max
                    if (maxTargetsPerCallSite > maxCallTargets) {
                        maxCallTargets = maxTargetsPerCallSite
                        methodWithMethodCallWithMaxTargets = method
                        methodCallWithMaxTargetsPC =
                            callees.find(e ⇒ e._2.size == maxCallTargets).get._1
                    }
                }
                println(f"Number of call sites: ${callGraph.callSites}%,d ")
                println(
                    f"Number of call edges: ${callGraph.callEdgesCount}%,d"+
                        f" / called-by edges: ${callGraph.calledByEdgesCount}%,d"
                )
                println(
                    "Maximum number of targets for one call: "+maxCallTargets+"; method: "+
                        methodWithMethodCallWithMaxTargets.fullyQualifiedSignature+
                        "; pc: "+methodCallWithMaxTargetsPC
                )
                println(
                    "Method with the maximum number of call sites: "+maxCallSitesPerMethod+"; method: "+
                        methodWithMaxCallSites.fullyQualifiedSignature
                )

                computedCallGraph

            } { m ⇒ println("Required memory for call graph: "+asMB(m))+"\n" }

        //
        // Let's create the visualization
        //
        val nodes: Set[DefaultMutableNode[Method]] = {

            val nodesForMethods = scala.collection.mutable.AnyRefMap.empty[Method, DefaultMutableNode[Method]]

            def createNode(caller: Method): DefaultMutableNode[Method] = {
                if (nodesForMethods.contains(caller))
                    return nodesForMethods(caller)

                val node = new DefaultMutableNode(caller, (m: Method) ⇒ m.toJava, {
                    if (caller.name == "<init>")
                        Some("darkseagreen1")
                    else if (caller.name == "<clinit>")
                        Some("darkseagreen")
                    else if (caller.isStatic)
                        Some("gold")
                    else
                        None
                })
                nodesForMethods += ((caller, node)) // break cycles!

                for {
                    perCallsiteCallees ← callGraph.calls(caller).values
                    callee ← perCallsiteCallees
                    if callee.classFile.fqn.startsWith(fqnFilter)
                } {
                    node.addChild(createNode(callee))
                }
                node
            }

            callGraph foreachCallingMethod { (method, callees) ⇒
                if (method.classFile.thisType.fqn.startsWith(fqnFilter))
                    createNode(method)
            }
            nodesForMethods.values.toSet[DefaultMutableNode[Method]] // it is a set already...
        }
        // The unresolved methods________________________________________________________:
        if (unresolvedMethodCalls.size > 0) {
            println("Unresolved method calls: "+unresolvedMethodCalls.size)
            //            val (umc, end) =
            //                if (unresolvedMethodCalls.size > 10)
            //                    (unresolvedMethodCalls.take(10), "\n\t...\n")
            //                else
            //                    (unresolvedMethodCalls, "\n")
            //            println(umc.mkString("Unresolved method calls:\n\t", "\n\t", end))
        }

        // The graph_____________________________________________________________________:
        //        val classFiles = project.classFiles.filter(_.thisType.fqn.startsWith(fqnFilter))
        //        val methods = classFiles.flatMap(_.methods)
        //        val consoleOutput = methods flatMap (m ⇒ callGraph.calls(m) flatMap { allCallees ⇒
        //            val (pc, callees) = allCallees
        //            for {
        //                callee ← callees
        //            } yield m.toJava+" => ["+pc+"] "+project.classFile(callee).thisType.toJava+"{ "+callee.toJava+" }"
        //        })
        //        println(consoleOutput.mkString("\n"))

        // The exceptions________________________________________________________________:
        if (exceptions.size > 0) {
            println("Exceptions: "+exceptions.size)
            //            println(exceptions.mkString("Exceptions that occured while analyzing...:\n\t", "\n\t", "\t"))
            writeAndOpen(
                exceptions.map(_.toFullString).mkString("Exceptions that occured while creating the call graph...:\n", "\n\n", ""),
                "ExceptionsThrownDuringCallGraphConstruction",
                ".txt"
            )
        }

        // Generate and show the graph
        writeAndOpen(toDot(nodes), callGraphAlgorithm+"CallGraph", ".dot")
        println("Callgraph:")
        println("Number of nodes: "+nodes.size)
        val edges = nodes.foldLeft(0) { (l, r) ⇒
            l + { var c = 0; r.foreachSuccessor(n ⇒ c += 1); c }
        }
        println("Number of edges: "+edges)

        // Write out the statistics about the calls relation
        //        writeAndOpen(
        //            callGraph.callsStatistics(),
        //            "CallGraphStatistics(calls)",
        //            ".tsv.txt")

        // Write out the statistics about the called-by relation
        //        writeAndOpen(
        //            callGraph.calledByStatistics(),
        //            "CallGraphStatistics(calledBy)",
        //            ".tsv.txt")
    }
}
