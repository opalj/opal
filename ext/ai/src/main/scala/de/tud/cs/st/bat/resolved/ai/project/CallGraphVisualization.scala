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
 * Enables the visualization of small call graphs built using CHA.
 *
 * @author Michael Eichberg
 */
object CallGraphVisualization {

    import de.tud.cs.st.util.debug._

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     *      or a directory containing the former. The second element must
     *      denote the name of a class and the third must denote the name of a method
     *      of the respective class. If the method is overloaded the first method
     *      is returned.
     */
    def main(args: Array[String]) {
        if (args.size != 2) {
            println("You have to specify the method that should be analyzed.")
            println("\t1) A jar/calss file or a directory containing jar/class files.")
            println("\t2) A pattern that specifies which classes should be included in the output.")
            sys.exit(-1)
        }

        Thread.sleep(8000)

        //
        // PROJECT SETUP
        //
        val project = PerformanceEvaluation.time {
            val fileName = args(0)
            val file = new java.io.File(fileName)
            if (!file.exists()) {
                println(Console.RED+"file does not exist: "+fileName + Console.RESET)
                return ;
            }
            val classFiles =
                try {
                    reader.Java7Framework.ClassFiles(file)
                } catch {
                    case e: Exception ⇒
                        println(Console.RED+"cannot read file: "+e.getMessage() + Console.RESET)
                        return ;
                }
            bat.resolved.analyses.IndexBasedProject(classFiles)
        } { t ⇒ println("Setting up the project took: "+nsToSecs(t)) }
        
        val classNameFilter = args(1)

        //
        // GRAPH CONSTRUCTION
        //
        val (callGraph, unresolvedMethodCalls) = PerformanceEvaluation.time {
            val callGraphFactory = new CallGraphFactory
            callGraphFactory.performCHA(
                project,
                callGraphFactory.defaultEntryPointsForCHA(project))
        } { t ⇒ println("Creating the call graph took: "+nsToSecs(t)) }
        import callGraph.calls
        println("Classes: "+project.classFiles.size)
        println("Methods: "+Method.methodsCount)
        println("Methods with more than one resolved call: "+calls.size)
        println("Methods which are called by at least one method: "+callGraph.calledBy.size)
        println("Unresolved method calls: "+unresolvedMethodCalls.size)
        //        println("Method with the most targets: "+{
        //            calls.view.map(_ map ())
        //        })
        // CallGraph( val calls: Map[Method, Map[PC, Set[Method]]]) )
        import de.tud.cs.st.util.graphs.{ toDot, SimpleNode, Node }

        val nodes: Set[Node] = {

            var nodesForMethods = Map.empty[Method, Node]

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
                nodesForMethods = nodesForMethods + ((caller, node)) // break cycles!

                for {
                    callees ← calls.get(caller)
                    perCallsiteCallees ← callees.values
                    callee ← perCallsiteCallees
                    if project.classFile(callee).thisClass.className.startsWith(classNameFilter)
                } {
                    node.addChild(createNode(callee))
                }
                node
            }

            calls.keySet.view.filter { method ⇒
                project.classFile(method).thisClass.className.startsWith(classNameFilter)
            } foreach { caller ⇒
                createNode(caller)
            }
            nodesForMethods.values.toSet[Node] // it is a set already...
        }
        // The unresolved methods________________________________________________________:
        //println(unresolvedMethodCalls.mkString("Unresolved calls:\n\t", "\n\t", ""))

        // The graph_____________________________________________________________________:
        //        val consoleOutput = callGraph.calls flatMap { caller ⇒
        //            for {
        //                (pc, callees) ← caller._2
        //                callee ← callees
        //            } yield caller._1.toJava+" => ["+pc+"] "+callee.toJava
        //        }
        //        println(consoleOutput.mkString("\n"))

        toDot.generateAndOpenDOT(nodes)

    }
}
