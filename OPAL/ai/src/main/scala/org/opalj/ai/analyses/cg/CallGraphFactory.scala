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
package analyses
package cg

import org.opalj.log.OPALLogger
import org.opalj.br._
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.concurrent.ThreadPoolN
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

/**
 * Factory object to create call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphFactory {

    @volatile var debug: Boolean = false

    /**
     * Returns a list of all entry points that is well suited if we want to
     * analyze a library/framework.
     *
     * The set of all entry points consists of:
     *  - all static initializers,
     *  - every non-private static method,
     *  - every non-private constructor,
     *  - every non-private method,
     *  - every private method (including a default constructor) related to
     *    Serialization, if the respective declaring class is a subtype of
     *    java.io.Serializable,
     *  // TODO ...
     *  - No entry points:
     *    public instance methods of a class that provides no way to create an instance of it (e.g., java.lang.Math)
     */
    def defaultEntryPointsForLibraries(project: SomeProject): Iterable[Method] = {
        val classHierarchy = project.classHierarchy
        val methods = new java.util.concurrent.ConcurrentLinkedQueue[Method]
        project.parForeachMethodWithBody(() ⇒ Thread.currentThread().isInterrupted()) { m ⇒
            val (_, classFile, method) = m
            if (!method.isPrivate ||
                ( // the method is private, but...
                    Method.isObjectSerializationRelated(method) &&
                    classHierarchy.isSubtypeOf(
                        classFile.thisType,
                        ObjectType.Serializable).isYesOrUnknown))
                methods.add(method)
        }
        import scala.collection.JavaConverters._
        methods.asScala
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
        findEntryPoints: () ⇒ Iterable[Method],
        configuration: CallGraphAlgorithmConfiguration): ComputedCallGraph = {
        implicit val logContext = theProject.logContext

        val entryPoints = findEntryPoints()
        if (entryPoints.isEmpty)
            return ComputedCallGraph.empty(theProject)

        import scala.collection.{ Map, Set }
        type MethodAnalysisResult = (( /*Caller*/ Method, Map[PC, /*Callees*/ Set[Method]]), List[UnresolvedMethodCall], Option[CallGraphConstructionException])

        import java.util.concurrent.Callable
        import java.util.concurrent.Executors
        import java.util.concurrent.ExecutorCompletionService

        val extractor = configuration.Extractor
        import extractor.extract

        /* START - EXECUTED CONCURRENTLY */
        def doAnalyzeMethod(method: Method): Callable[MethodAnalysisResult] =
            new Callable[MethodAnalysisResult] {
                def call(): MethodAnalysisResult = {
                    val classFile = theProject.classFile(method)
                    try {
                        val (callEdges, unresolveableMethodCalls) =
                            extract(theProject, classFile, method)
                        (callEdges, unresolveableMethodCalls, None)
                    } catch {
                        case ct: scala.util.control.ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒
                            (
                                (method, Map.empty[PC, /*Callees*/ Set[Method]]),
                                List.empty,
                                Some(CallGraphConstructionException(classFile, method, t))
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
        val completionService =
            new ExecutorCompletionService[MethodAnalysisResult](
                ThreadPoolN(Math.max(NumberOfThreadsForCPUBoundTasks - 1, 1))
            )

        @inline def submitMethod(method: Method): Unit = {
            if (methodSubmitted.contains(method))
                return ;

            methodSubmitted += method

            var minimumSize = 4
            // the minimum length of a method that may call another method is 4
            // - call to a static method without args (3 bytes)
            // - a return (void or the result of the static method call)
            val instructions = method.body.get.instructions
            if (instructions.size < minimumSize)
                return ;

            if (instructions(0).opcode != INVOKESTATIC.opcode) {
                minimumSize = 5
                val secondInstruction = instructions(1)
                if (secondInstruction != null && (
                    secondInstruction.opcode < 182 ||
                    secondInstruction.opcode > 186))
                    minimumSize = 6
            }
            if (instructions.size < minimumSize)
                return ;

            futuresCount += 1
            completionService.submit(doAnalyzeMethod(method))
        }

        // Initialization
        entryPoints foreach { method ⇒ submitMethod(method) }

        val builder = new CallGraphBuilder(theProject)
        var exceptions = List.empty[CallGraphConstructionException]
        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        var analyzedMethods = 0
        while (futuresCount > 0) {
            // 1. GET NEXT RESULT
            val (callSite @ (_ /*method*/ , callEdges), moreUnresolvedMethodCalls, exception) =
                completionService.take().get()
            futuresCount -= 1
            analyzedMethods += 1

            if (debug && (analyzedMethods % 1000 == 0)) {
                OPALLogger.info(
                    "progress - call graph",
                    s"analyzed: $analyzedMethods methods")
            }

            // 2. ENQUE NEXT METHODS
            if (callEdges.nonEmpty) {
                callEdges.foreach(_._2.foreach { m ⇒
                    // A body may not be defined in two cases:
                    // 1. the method is native
                    // 2. the method belongs to the library and is loaded using the
                    //    library class file loader which drops the implementation of
                    //    methods.
                    if (m.body.isDefined) {
                        submitMethod(m)
                    }
                })
            }

            // 3. PROCESS RESULTS
            if (moreUnresolvedMethodCalls.nonEmpty)
                unresolvedMethodCalls = moreUnresolvedMethodCalls ::: unresolvedMethodCalls
            if (exception.isDefined)
                exceptions = exception.get :: exceptions
            builder.addCallEdges(callSite)
        }

        // TODO use log
        if (debug)
            OPALLogger.info(
                "progress - call graph",
                "finished analzying the bytecode, constructing the final call graph")

        ComputedCallGraph(
            builder.buildCallGraph,
            findEntryPoints,
            unresolvedMethodCalls,
            exceptions)
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

