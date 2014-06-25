/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package project

import org.opalj.br.{ Method }
import org.opalj.br.analyses.Project
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8FrameworkWithCaching

/**
 * Visualizes call graphs using Graphviz.
 *
 * Given GraphViz's capabilities only small call graphs (10 to 20 nodes) can
 * be effectively visualized.
 *
 * @author Michael Eichberg
 */
object CallGraphVisualization {

    import java.net.URL

    import scala.Console.{ err, RED, RESET }

    import org.opalj.util.PerformanceEvaluation.{ time, memory, asMB, ns2sec }

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     *      or a directory containing the former. The second element must
     *      denote the beginning of a package name and will be used to filter
     *      the methods that are included in the call graph.
     */
    def main(args: Array[String]) {
        if ((args.size < 3) || (args.size > 4)) {
            println("You have to specify:")
            println("\t1) The algorithm to use (CHA or VTA).")
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
                Thread.sleep(secs * 1000)
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
                                println(RED+"cannot read file: "+e.getMessage() + RESET)
                                sys.exit(-3)
                        }
                    cache = null
                    ClassFileReader = null
                    Project(classFiles)
                } { t ⇒ println("Setting up the project took: "+ns2sec(t)) }
            } { m ⇒ println("Required memory for base representation: "+asMB(m)) }
        val fqnFilter = args(2)

        println(project.statistics.map(e ⇒ "\t"+e._1+": "+e._2).mkString("Project statistics:\n\t", "\n\t", ""))

        //
        // GRAPH CONSTRUCTION
        //
        import CallGraphFactory.defaultEntryPointsForLibraries
        val callGraphAlgorithm = args(0)
        val ComputedCallGraph(callGraph, unresolvedMethodCalls, exceptions) =
            memory {
                time {
                    val callGraphAlgorithmConfig = args(0) match {
                        case "VTA" ⇒
                            new VTACallGraphAlgorithmConfiguration()
                        case _ /*CHA*/ ⇒
                            new CHACallGraphAlgorithmConfiguration()
                    }
                    val entryPoints = defaultEntryPointsForLibraries(project)
                    val computedCallGraph = CallGraphFactory.create(
                        project,
                        entryPoints,
                        callGraphAlgorithmConfig)
                    computedCallGraph
                } { t ⇒ println("Creating the call graph took: "+ns2sec(t)) }
            } { m ⇒ println("Required memory for call graph: "+asMB(m)) }

        // Some statistics 
        import callGraph.{ calls, callsCount, calledByCount, foreachCallingMethod }
        println("Methods with at least one resolved call: "+callsCount)
        println("Methods which are called by at least one method: "+calledByCount)

        var callGraphEdgesCount = 0
        var maxCallSitesPerMethod = 0
        var maxTargets = 0
        foreachCallingMethod { (method, callees) ⇒
            val calleesCount = callees.size
            callGraphEdgesCount += calleesCount
            if (calleesCount > maxCallSitesPerMethod) maxCallSitesPerMethod = calleesCount
            for (targets ← callees.values) {
                val targetsCount = targets.size
                if (targetsCount > maxTargets) maxTargets = targetsCount
            }
        }
        println("Number of all call edges: "+callGraphEdgesCount)
        println("Maximum number of targets over all calls: "+maxTargets)
        println("Maximum number of method calls over all methods: "+maxCallSitesPerMethod)

        //
        // Let's create the visualization
        //
        import org.opalj.util.writeAndOpenDesktopApplication
        import org.opalj.graphs.{ toDot, SimpleNode, Node }
        val nodes: Set[Node] = {

            var nodesForMethods = scala.collection.mutable.AnyRefMap.empty[Method, Node]

            def createNode(caller: Method): Node = {
                if (nodesForMethods.contains(caller))
                    return nodesForMethods(caller)

                val node = new SimpleNode(
                    caller,
                    (m: Method) ⇒ project.classFile(m).thisType.toJava+" { "+m.toJava+" } ",
                    {
                        if (caller.name == "<init>")
                            Some("darkseagreen1")
                        else if (caller.name == "<clinit>")
                            Some("darkseagreen")
                        else if (caller.isStatic)
                            Some("gold")
                        else
                            None
                    }
                )
                nodesForMethods += ((caller, node)) // break cycles!

                for {
                    perCallsiteCallees ← calls(caller).values
                    callee ← perCallsiteCallees
                    if project.classFile(callee).fqn.startsWith(fqnFilter)
                } {
                    node.addChild(createNode(callee))
                }
                node
            }

            foreachCallingMethod { (method, callees) ⇒
                if (project.classFile(method).thisType.fqn.startsWith(fqnFilter))
                    createNode(method)
            }
            nodesForMethods.values.toSet[Node] // it is a set already...
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
            writeAndOpenDesktopApplication(
                exceptions.map(_.toFullString).mkString("Exceptions that occured while creating the call graph...:\n", "\n\n", ""),
                "Logged exceptions", ".txt"
            )
        }

        // Generate and show the graph
        //        writeAndOpenDesktopApplication(
        //            toDot.generateDot(nodes),
        //            callGraphAlgorithm+"CallGraph", ".dot")
        println("Callgraph:")
        println("Number of nodes: "+nodes.size)
        val edges = nodes.foldLeft(0) { (l, r) ⇒
            l + { var c = 0; r.foreachSuccessor(n ⇒ c += 1); c }
        }
        println("Number of edges: "+edges)

        // Write out the statistics about the calls relation
        //        writeAndOpenDesktopApplication(
        //            callGraph.callsStatistics(),
        //            "CallGraphStatistics(calls)",
        //            ".tsv.txt")

        // Write out the statistics about the called-by relation
        //        writeAndOpenDesktopApplication(
        //            callGraph.calledByStatistics(),
        //            "CallGraphStatistics(calledBy)",
        //            ".tsv.txt")
    }
}
