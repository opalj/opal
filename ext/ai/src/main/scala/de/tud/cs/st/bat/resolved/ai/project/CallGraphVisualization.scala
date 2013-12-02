/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai
package project

/**
 * Visualizes call graphs using Graphviz.
 *
 * Given GraphViz's capabilities only small call graphs (10 to 20 nodes) can
 * be effectively visualized.
 *
 * @author Michael Eichberg
 */
object CallGraphVisualization {

    import de.tud.cs.st.util.debug._
    import de.tud.cs.st.util.debug.PerformanceEvaluation.{ time, memory, asMB }
    import Console._

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     *      or a directory containing the former. The second element must
     *      denote the beginning of a package name and will be used to filter
     *      the methods that are included in the call graph.
     */
    def main(args: Array[String]) {
        if ((args.size < 2) || (args.size > 3)) {
            println("You have to specify the method that should be analyzed.")
            println("\t1) A jar/class file or a directory containing jar/class files.")
            println("\t2) A pattern that specifies which classes should be included in the output.")
            println("\t3) The number of seconds (max. 30) before the analysis starts (e.g., to attach a profiler).")
            sys.exit(-1)
        }

        // To make it possible to attach a profiler: 
        if (args.size == 3) {
            try {
                val secs = Integer.parseInt(args(2), 10)
                if (secs > 30) {
                    Console.err.println("\t3) The number of seconds before the analysis starts (e.g., to attach a profiler).")
                    sys.exit(-30)
                }
                Thread.sleep(secs * 1000)
            } catch {
                case _: NumberFormatException ⇒
                    Console.err.println("\t3) The number of seconds before the analysis starts (e.g., to attach a profiler).")
                    sys.exit(-31)
            }
        }
        //

        //
        // PROJECT SETUP
        //
        val project =
            memory {
                time {
                    val fileName = args(0)
                    val file = new java.io.File(fileName)
                    if (!file.exists()) {
                        println(RED+"file does not exist: "+fileName + RESET)
                        sys.exit(-2)
                    }
                    val classFiles =
                        try {
                            reader.Java7Framework.ClassFiles(file)
                        } catch {
                            case e: Exception ⇒
                                println(RED+"cannot read file: "+e.getMessage() + RESET)
                                sys.exit(-3)
                        }
                    bat.resolved.analyses.IndexBasedProject(classFiles)
                } { t ⇒ println("Setting up the project took: "+nsToSecs(t)) }
            } { m ⇒ println("Required memory for base representation: "+asMB(m)) }
        val classNameFilter = args(1)

        //
        // GRAPH CONSTRUCTION
        //
        val (callGraph, unresolvedMethodCalls, exceptions) =
            memory {
                time {
                    CallGraphFactory.create(
                        project,
                        CallGraphFactory.defaultEntryPointsForLibraries(project),
                        new CHACallGraphAlgorithmConfiguration())
                } { t ⇒ println("Creating the call graph took: "+nsToSecs(t)) }
            } { m ⇒ println("Required memory for call graph: "+asMB(m)) }

        // Some statistics 
        import callGraph.{ calls, callsCount, calledByCount, foreachCallingMethod }
        println("Classes: "+project.classFiles.size)
        println("Methods: "+Method.methodsCount)
        println("Methods with at least one resolved call: "+callsCount)
        println("Methods which are called by at least one method: "+calledByCount)

        var maxCallSitesPerMethod = 0
        var maxTargets = 0
        foreachCallingMethod { (method, callees) ⇒
            val calleesCount = callees.size
            if (calleesCount > maxCallSitesPerMethod) maxCallSitesPerMethod = calleesCount
            for (targets ← callees.values) {
                val targetsCount = targets.size
                if (targetsCount > maxTargets) maxTargets = targetsCount
            }
        }
        println("Maximum number of targets over all calls: "+maxTargets)
        println("Maximum number of method calls over all methods: "+maxCallSitesPerMethod)

        //
        // Let's create visualization
        //
        import de.tud.cs.st.util.graphs.{ toDot, SimpleNode, Node }
        val nodes: Set[Node] = {

            var nodesForMethods = scala.collection.mutable.HashMap.empty[Method, Node]

            def createNode(caller: Method): Node = {
                if (nodesForMethods.contains(caller))
                    return nodesForMethods(caller)

                val node = new SimpleNode(
                    caller,
                    (m: Method) ⇒ project.classFile(m).thisClass.toJava+" { "+m.toJava+" } ",
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
                    callees ← calls(caller)
                    perCallsiteCallees ← callees.values
                    callee ← perCallsiteCallees
                    if project.classFile(callee).className.startsWith(classNameFilter)
                } {
                    node.addChild(createNode(callee))
                }
                node
            }

            foreachCallingMethod { (method, callees) ⇒
                if (project.classFile(method).thisClass.className.startsWith(classNameFilter))
                    createNode(method)
            }
            nodesForMethods.values.toSet[Node] // it is a set already...
        }
        // The unresolved methods________________________________________________________:
        if (unresolvedMethodCalls.size > 0) {
            println("Unresolved method calls: "+unresolvedMethodCalls.size)
            val (umc, end) =
                if (unresolvedMethodCalls.size > 10)
                    (unresolvedMethodCalls.take(10), "\n\t...\n")
                else
                    (unresolvedMethodCalls, "\n")
            println(umc.mkString("Unresolved method calls:\n\t", "\n\t", end))
        }

        // The graph_____________________________________________________________________:
        //        val consoleOutput = callGraph.calls flatMap { caller ⇒
        //            for {
        //                (pc, callees) ← caller._2
        //                callee ← callees
        //            } yield caller._1.toJava+" => ["+pc+"] "+callee.toJava
        //        }
        //        println(consoleOutput.mkString("\n"))

        // The exceptions________________________________________________________________:
        println("Exceptions: "+exceptions.size)
        println(exceptions.mkString("Exceptions:\n\t", "\n\t", "\t"))

        // Generate and show the graph
        toDot.generateAndOpenDOT("CallGraph", nodes)

        // Write out the statistics about the calls relation
        writeAndOpenDesktopApplication(
            callGraph.callsStatistics(),
            "CallGraphStatistics(calls)",
            ".tsv.txt")

        // Write out the statistics about the called-by relation
        writeAndOpenDesktopApplication(
            callGraph.calledByStatistics(),
            "CallGraphStatistics(calledBy)",
            ".tsv.txt")
    }
}
