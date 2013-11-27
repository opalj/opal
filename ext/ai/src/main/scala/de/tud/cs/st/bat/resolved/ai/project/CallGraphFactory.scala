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

import bat.resolved.analyses.{ SomeProject, Project }
import bat.resolved.ai.domain._

/**
 * Factory to create call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphFactory {

    /**
     * Returns a list of all entry points that is well suited if we want to
     * analyze a library/framework.
     *
     * The set of all entry points consists of:
     * - all static initializers,
     * - every non-private static method,
     * - every non-private constructor,
     * - every non-private method.
     */
    def defaultEntryPointsForLibraries(project: SomeProject): List[Method] = {
        var entryPoints = List.empty[Method]
        project.foreachMethod { method: Method ⇒
            if (!method.isPrivate && method.body.isDefined)
                entryPoints = method :: entryPoints
        }
        entryPoints
    }

    /**
     * Creates a call graph using ''Class Hierarchy Analysis''.
     * The call graph is created by analyzing each entry point on its own. Hence,
     * the call graph is calculated under a specific assumption about a
     * programs/libraries/framework's entry methods.
     *
     * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to
     * `java.lang.Object`.
     */
    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method]): (CallGraph[Source], List[UnresolvedMethodCall], List[CallGraphConstructionException]) = {

    	type MethodAnalysisResult = (List[(Method, PC, Iterable[Method])], List[UnresolvedMethodCall], Option[CallGraphConstructionException])
    	
        import java.util.concurrent.Callable
        import java.util.concurrent.Future
        import java.util.concurrent.Executors

        val cache = new CallGraphCache[MethodSignature, Iterable[Method]]

        /* START - EXECUTED CONCURRENTLY */
        def doAnalyzeMethod(method: Method): Callable[MethodAnalysisResult] =
            new Callable[MethodAnalysisResult] {
                def call(): MethodAnalysisResult = {
                    val classFile = theProject.classFile(method)
                    val domain: CHACallGraphDomain[Source, Int] =
                        new DefaultCHACallGraphDomain(theProject, cache, classFile, method)
                    try {
                        BaseAI(classFile, method, domain)
                        (domain.callEdges, domain.unresolvedMethodCalls, None)
                    } catch {
                        case exception: Exception ⇒
                            (
                                domain.callEdges,
                                domain.unresolvedMethodCalls,
                                Some(CallGraphConstructionException(classFile, method, exception))
                            )
                    }
                }
            }
        /* END - EXECUTED CONCURRENTLY */

        var futures = List.empty[Future[MethodAnalysisResult]] // OUR WORKLIST
        val methodSubmitted: Array[Boolean] = new Array(Method.methodsCount)
        val processors = Runtime.getRuntime().availableProcessors()
        val executorService = Executors.newFixedThreadPool(processors)
        @inline def submitMethod(method: Method): Unit = {
            if (methodSubmitted(method.id))
                return
            else
                methodSubmitted(method.id) = true

            futures = executorService.submit(doAnalyzeMethod(method)) :: futures
        }

        // Initialization
        entryPoints.foreach(method ⇒ submitMethod(method))

        val builder = new CallGraphBuilder[Source](theProject)
        var exceptions = List.empty[CallGraphConstructionException]
        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        while (futures.nonEmpty) {
            val future = futures.head
            futures = futures.tail
            val (callEdges, moreUnresolvedMethodCalls, exception) = future.get()
            if (moreUnresolvedMethodCalls.nonEmpty)
                unresolvedMethodCalls = moreUnresolvedMethodCalls ::: unresolvedMethodCalls
            if (exception.isDefined)
                exceptions = exception.get :: exceptions
            callEdges.foreach(_._3.foreach { method ⇒
                if (!method.isNative) submitMethod(method)
            })
            builder.addCallEdges(callEdges)
        }
        executorService.shutdown()

        (builder.buildCallGraph, unresolvedMethodCalls, exceptions)
    }
}


/*
Things that complicate matters for more complex call graph analyses:
class A {

    private A a = this;

    public m() {    
        a.foo() // here, a refers to an object of type B if bar was called before m()
        a.foo() // here, a "always" refers to an object of type B and not this!
    }

    private foo() {
        a = new B();
    }

    public bar() {
        a = new B();
    }
} 
class B extends A {
    private foo() {
        bar()
    }

    public bar() {
        // do nothing
    }
}
*/ 

 


