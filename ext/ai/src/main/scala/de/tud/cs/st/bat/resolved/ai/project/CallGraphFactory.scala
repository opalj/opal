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

// internally, we use mutable data structures (Arrays, mutable Maps) due to their 
// better performance characteristics, but they are not exposed to the outside
import collection.mutable.HashMap

import bat.resolved.ai.domain._

/**
 * Factory methods to create call graphs.
 *
 * @author Michael Eichberg
 */
object CallGraphFactory {

    import language.existentials

    /**
     * The set of all entry points consists of:
     * - all static initializers,
     * - every non-private static method,
     * - every non-private constructor,
     * - every non-private method.
     */
    def defaultEntryPointsForCHA(project: SomeProject): List[Method] = {
        var entryPoints = List.empty[Method]
        project.foreachMethod { method: Method ⇒
            if (!method.isPrivate && method.body.isDefined)
                entryPoints = method :: entryPoints
        }
        entryPoints
    }

    /**
     * Creates a call graph using Class Hierarchy Analysis.
     * The call graph is created by analyzing each entry point on its own. The call
     * graph is calculated under a specific assumption about a programs/libraries/framework's
     * entry methods.
     *
     * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to
     * `java.lang.Object`.
     */
    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method]): (CHACallGraph[Source], List[UnresolvedMethodCall], List[CallGraphConstructionException]) = {

        val exceptionsMutex = new Object
        var exceptions = List.empty[CallGraphConstructionException]
        def handleException(classFile: ClassFile, method: Method, exception: Exception) {
            exceptionsMutex.synchronized {
                exceptions =
                    CallGraphConstructionException(classFile, method, exception) ::
                        exceptions
            }
        }

        val unresolvedMethodCallsMutex = new Object
        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        def handleUnresolvedMethodCall(
            callerClass: ReferenceType,
            caller: Method,
            pc: PC,
            calleeClass: ReferenceType,
            calleeName: String,
            calleeDescriptor: MethodDescriptor) {
            unresolvedMethodCallsMutex.synchronized {
                unresolvedMethodCalls =
                    UnresolvedMethodCall(
                        callerClass, caller, pc,
                        calleeClass, calleeName, calleeDescriptor) :: unresolvedMethodCalls
            }
        }

        (
            performCHA(theProject, entryPoints, handleUnresolvedMethodCall _, handleException _),
            unresolvedMethodCalls,
            exceptions
        )
    }

    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method],
        handleUnresolvedMethodCall: ( /*callerClass: */ ReferenceType, /*caller:*/ Method, /*pc:*/ PC, /*calleeClass:*/ ReferenceType, /*calleeName:*/ String, /*calleeDescriptor: */ MethodDescriptor) ⇒ _,
        handleException: (ClassFile, Method, Exception) ⇒ _): CHACallGraph[Source] = {

        val methodsCount = Method.methodsCount

        val context = new CallGraphConstructionContext[Source](
            theProject,
            /* methodsToAnalyze */ entryPoints,
            /* resolvedMethodsCache */
            new Array[HashMap[MethodSignature, Iterable[Method]]](theProject.objectTypesCount),
            handleUnresolvedMethodCall,
            /* calledBy */ new Array[HashMap[Method, Set[PC]]](methodsCount),
            /* calls */ new Array[HashMap[PC, Iterable[Method]]](methodsCount)
        )
        import context._

        var method: Method = null
        while ({ method = context.takeMethod; method != null }) {
            val classFile = theProject.classFile(method)
            try {
                BaseAI(
                    classFile, method,
                    new DefaultCHACallGraphDomain(context, classFile, method)
                )
            } catch {
                case e: Exception ⇒ handleException(classFile, method, e)
            }
        }

        new CHACallGraph(theProject, calledByMap, callsMap)
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

 


