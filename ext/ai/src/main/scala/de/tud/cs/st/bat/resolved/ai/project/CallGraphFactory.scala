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
     * Creates a call graph using the configured call graph algorithm.
     *
     * The call graph is created by analyzing each method using a new instance
     * of a domain. Furthermore, the methods are analyzed in parallel. Hence,
     * the call graph algorithm (and its used cache) have to be thread-safe.
     */
    def create[Source](
        theProject: Project[Source],
        entryPoints: List[Method],
        configuration: CallGraphAlgorithmConfiguration[Source]): (CallGraph[Source], List[UnresolvedMethodCall], List[CallGraphConstructionException]) = {

        type MethodAnalysisResult = (List[( /*Caller*/ Method, PC, /*Callees*/ Iterable[Method])], List[UnresolvedMethodCall], Option[CallGraphConstructionException])

        import java.util.concurrent.Callable
        import java.util.concurrent.Future
        import java.util.concurrent.Executors
        import java.util.concurrent.ExecutorCompletionService

        val cache = configuration.Cache()

        /* START - EXECUTED CONCURRENTLY */
        def doAnalyzeMethod(method: Method): Callable[MethodAnalysisResult] =
            new Callable[MethodAnalysisResult] {
                def call(): MethodAnalysisResult = {
                    val classFile = theProject.classFile(method)
                    val domain = configuration.Domain(theProject, cache, classFile, method)
                    try {
                        BaseAI(classFile, method, domain)
                        (domain.allCallEdges, domain.allUnresolvedMethodCalls, None)
                    } catch {
                        case exception: Exception ⇒
                            (
                                domain.allCallEdges,
                                domain.allUnresolvedMethodCalls,
                                Some(CallGraphConstructionException(classFile, method, exception))
                            )
                    }
                }
            }
        /* END - EXECUTED CONCURRENTLY */

        var futuresCount = 0
        val methodSubmitted: Array[Boolean] = new Array(Method.methodsCount)
        val executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() /*+ 1*/)
        val completionService =
            new ExecutorCompletionService[MethodAnalysisResult](executorService)
        @inline def submitMethod(method: Method): Unit = {
            if (methodSubmitted(method.id))
                return

            methodSubmitted(method.id) = true
            futuresCount += 1
            completionService.submit(doAnalyzeMethod(method))
        }

        // Initialization
        entryPoints.foreach(method ⇒ submitMethod(method))

        val builder = new CallGraphBuilder[Source](theProject)
        var exceptions = List.empty[CallGraphConstructionException]
        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        while (futuresCount > 0) {
            // 1. GET NEXT RESULT
            val (callEdges, moreUnresolvedMethodCalls, exception) =
                completionService.take().get()
            futuresCount -= 1

            // 2. ENQUE NEXT METHODS
            if (callEdges.nonEmpty)
                callEdges.foreach(_._3.foreach { m ⇒ if (!m.isNative) submitMethod(m) })

            // 3. PROCESS RESULTS
            if (moreUnresolvedMethodCalls.nonEmpty)
                unresolvedMethodCalls = moreUnresolvedMethodCalls ::: unresolvedMethodCalls
            if (exception.isDefined)
                exceptions = exception.get :: exceptions
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

 


