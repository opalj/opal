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
import collection.Set
import collection.Map
import collection.mutable.HashMap
import bat.resolved.ai.domain._
import scala.annotation.tailrec
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * Factory methods to create call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphFactory {

    import language.existentials

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
     * Creates a call graph using Class Hierarchy Analysis.
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

        val exceptionsMutex = new Object
        var exceptions = List.empty[CallGraphConstructionException]
        def caughtException(classFile: ClassFile, method: Method, exception: Exception) {
            exceptionsMutex.synchronized {
                exceptions =
                    CallGraphConstructionException(classFile, method, exception) ::
                        exceptions
            }
        }

        val unresolvedMethodCallsMutex = new Object
        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        def unresolvedMethodCall(
            callerClass: ReferenceType,
            caller: Method,
            pc: PC,
            calleeClass: ReferenceType,
            calleeName: String,
            calleeDescriptor: MethodDescriptor): Unit = {
            unresolvedMethodCallsMutex.synchronized {
                unresolvedMethodCalls =
                    UnresolvedMethodCall(
                        callerClass, caller, pc,
                        calleeClass, calleeName, calleeDescriptor) :: unresolvedMethodCalls
            }
        }

        (
            performCHA(theProject, entryPoints, unresolvedMethodCall _, caughtException _),
            unresolvedMethodCalls,
            exceptions
        )
    }

    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method],
        unresolvedMethodCall: ( /*callerClass: */ ReferenceType, /*caller:*/ Method, /*pc:*/ PC, /*calleeClass:*/ ReferenceType, /*calleeName:*/ String, /*calleeDescriptor: */ MethodDescriptor) ⇒ Unit,
        caughtException: (ClassFile, Method, Exception) ⇒ _): CallGraph[Source] = {

        val cache = CHACache(theProject)

        var futuresMutex = new Object
        var futures = List.empty[Future[List[(Method, PC, Iterable[Method])]]]

        val methodSubmitted: Array[Boolean] = new Array(Method.methodsCount)
        val executorService =
            java.util.concurrent.Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors())

        def submitMethod(method: Method): Unit = {
            methodSubmitted.synchronized {
                if (methodSubmitted(method.id))
                    return
                else
                    methodSubmitted(method.id) = true
            }
            val newFuture = executorService.submit(doAnalyzeMethod(method))
            futuresMutex.synchronized {
                futures = newFuture :: futures
            }
        }

        def doAnalyzeMethod(
            method: Method): Callable[List[(Method, PC, Iterable[Method])]] =
            new Callable[List[(Method, PC, Iterable[Method])]] {
                def call(): List[(Method, PC, Iterable[Method])] = {
                    val classFile = theProject.classFile(method)
                    try {
                        val domain =
                            new DefaultCHACallGraphDomain(
                                theProject, cache,
                                classFile, method,
                                unresolvedMethodCall
                            )
                        BaseAI(classFile, method, domain)
                        domain.callEdges
                    } catch {
                        case e: Exception ⇒
                            caughtException(classFile, method, e)
                            List.empty
                    }
                }
            }

        entryPoints.foreach(method ⇒ submitMethod(method))

        val builder = new CallGraphBuilder[Source](theProject)
        while (futures.nonEmpty) {
            val future = futuresMutex.synchronized {
                val future = futures.head
                futures = futures.tail
                future
            }
            val callEdges = future.get()
            callEdges.foreach(_._3.foreach(method => if(!method.isNative) submitMethod(method)))
            builder.addCallEdges(callEdges)
        }
        executorService.shutdown()

        builder.buildCallGraph
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

 


