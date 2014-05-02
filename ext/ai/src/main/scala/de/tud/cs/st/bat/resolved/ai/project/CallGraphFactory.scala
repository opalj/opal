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
package de.tud.cs.st
package bat
package resolved
package ai
package project

import bat.resolved.analyses.SomeProject
import bat.resolved.ai.domain._

case class ComputedCallGraph(
    callGraph: CallGraph,
    unresolvedMethodCalls: List[UnresolvedMethodCall],
    constructionExceptions: List[CallGraphConstructionException])

/**
 * Factory object to create call graphs.
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
        for {
            classFile ← project.projectClassFiles
            method ← classFile.methods
            if method.body.isDefined
            if !method.isPrivate
        } yield {
            method
        }
    }

    /**
     * Creates a call graph using the configured call graph algorithm.
     *
     * The call graph is created by analyzing each method using a new instance
     * of a domain. Furthermore, the methods are analyzed in parallel. Hence,
     * the call graph algorithm (and its used cache) have to be thread-safe.
     */
    def create(
        theProject: SomeProject,
        entryPoints: List[Method],
        configuration: CallGraphAlgorithmConfiguration): ComputedCallGraph = {

        if (entryPoints.isEmpty) {
            Console.err.println("The call graph has no entry points!")
        }
        import scala.collection.{ Map, Set }
        type MethodAnalysisResult = (( /*Caller*/ Method, Map[PC, /*Callees*/ Set[Method]]), List[UnresolvedMethodCall], Option[CallGraphConstructionException])

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
        val methodSubmitted: scala.collection.mutable.HashSet[Method] =
            new scala.collection.mutable.HashSet[Method]() {
                override def initialSize: Int = theProject.methodsCount
            }
        val executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() /*+ 1*/ )
        val completionService =
            new ExecutorCompletionService[MethodAnalysisResult](executorService)

        @inline def submitMethod(method: Method): Unit = {
            if (methodSubmitted.contains(method))
                return

            methodSubmitted += (method)
            futuresCount += 1
            completionService.submit(doAnalyzeMethod(method))
        }

        // Initialization
        entryPoints foreach { method ⇒ submitMethod(method) }

        val builder = new CallGraphBuilder(theProject)
        var exceptions = List.empty[CallGraphConstructionException]
        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        while (futuresCount > 0) {
            // 1. GET NEXT RESULT
            val (callSite @ (method, callEdges), moreUnresolvedMethodCalls, exception) =
                completionService.take().get()
            futuresCount -= 1

            // 2. ENQUE NEXT METHODS
            if (callEdges.nonEmpty) {
                callEdges.foreach(_._2.foreach { m ⇒ if (!m.isNative) submitMethod(m) })
            }

            // 3. PROCESS RESULTS
            if (moreUnresolvedMethodCalls.nonEmpty)
                unresolvedMethodCalls = moreUnresolvedMethodCalls ::: unresolvedMethodCalls
            if (exception.isDefined)
                exceptions = exception.get :: exceptions
            builder.addCallEdges(callSite)
        }
        executorService.shutdown()

        ComputedCallGraph(builder.buildCallGraph, unresolvedMethodCalls, exceptions)
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

 


