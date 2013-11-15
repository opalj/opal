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

import bat.resolved.analyses.{ Project, ReportableAnalysisResult }

import collection.Set
import collection.Map

/**
 * Creates a call graph by analyzing each entry point on its own. The call
 * graph is calculated under a specific assumption about a programs/libraries/framework's
 * entry methods.
 *
 * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to
 * `java.lang.Object`.
 *
 * @author Michael Eichberg
 */
class CallGraphFactory {

    import language.existentials

    // internally, we use mutable data structures due to their better performance 
    // characteristics, but they are not exposed to the outside
    import collection.mutable.HashSet
    import collection.mutable.HashMap

    import bat.resolved.ai.domain._

    /**
     * The set of all entry points consists of:
     * - all static initializers,
     * - every non-private static method,
     * - every non-private constructor,
     * - every non-private method.
     */
    def defaultEntryPointsForCHA(project: Project[_]): List[Method] = {
        var entryPoints = List.empty[Method]
        project.foreachMethod { method: Method ⇒
            if (!method.isPrivate && method.body.isDefined)
                entryPoints = method :: entryPoints
        }
        entryPoints
    }

    def performCHA[Source](
        theProject: Project[Source],
        entryPoints: List[Method]): (CHACallGraph, Seq[UnresolvedMethodCall]) = {

        type DomainContext = Int

        val calledBy =
            //            HashMap.empty[Method, HashMap[Method, collection.mutable.Set[PC]]]
            new HashMap[Method, HashMap[Method, collection.mutable.Set[PC]]] {
                override def initialSize = Method.methodsCount
            }
        val calls =
            //            HashMap.empty[Method, HashMap[PC, collection.mutable.Set[Method]]]
            new HashMap[Method, HashMap[PC, collection.mutable.Set[Method]]] {
                override def initialSize = Method.methodsCount
            }
        val analyzedMethods = new Array[Boolean](Method.methodsCount)
        var methodsToAnalyze = entryPoints

        var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]
        def unresolvedMethodCall(
            callerClass: ReferenceType,
            caller: Method,
            pc: PC,
            calleeClass: ReferenceType,
            calleeName: String,
            calleeDescriptor: MethodDescriptor) {
            unresolvedMethodCalls =
                UnresolvedMethodCall(
                    callerClass, caller, pc,
                    calleeClass, calleeName, calleeDescriptor) :: unresolvedMethodCalls
        }

        // This domain does not have any associated state. 
        class MethodDomain(
            val callerClassFile: ClassFile,
            val caller: Method)
                extends Domain[DomainContext]
                with DefaultDomainValueBinding[DomainContext]
                with DefaultTypeLevelIntegerValues[DomainContext]
                with DefaultTypeLevelLongValues[DomainContext]
                with DefaultTypeLevelFloatValues[DomainContext]
                with DefaultTypeLevelDoubleValues[DomainContext]
                with DefaultTypeLevelReferenceValues[DomainContext]
                //with DefaultPreciseReferenceValues[DomainContext]
                //with StringValues[DomainContext]
                with TypeLevelArrayInstructions
                with TypeLevelFieldAccessInstructions
                with TypeLevelInvokeInstructions
                with DoNothingOnReturnFromMethod
                with ProjectBasedClassHierarchy[Source] {

            def identifier = caller.id

            def project: Project[Source] = theProject

            @inline private[this] def addCallEdge(
                caller: Method,
                pc: PC,
                callee: Method): Unit = {
                // calledBy: Map[Method, Map[Method, Set[PC]]]
                {
                    val callers = calledBy.getOrElseUpdate(callee, HashMap.empty)
                    val callSites = callers.getOrElseUpdate(caller, HashSet.empty)
                    callSites add pc
                }

                // calls : Map[Method, Map[PC, Set[Method]]]          
                {
                    val callSites = calls.getOrElseUpdate(caller, HashMap.empty)
                    val callees = callSites.getOrElseUpdate(pc, HashSet.empty)
                    callees add callee
                }

                if (!callee.isNative) {
                    assume(
                        callee.body.isDefined,
                        "call edge to an abstract method"+caller.toJava+"=>"+callee.toJava)
                    methodsToAnalyze = callee :: methodsToAnalyze
                }
            }

            @inline private[this] def dynamicallyBoundInvocation(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor) {
                // PLAIN CHA - we do not consider any data-flow information 
                // that is available
                val callees = classHierarchy.lookupImplementingMethods(
                    declaringClass, name, descriptor, project
                )
                if (callees.isEmpty)
                    unresolvedMethodCall(
                        callerClassFile.thisClass, caller, pc,
                        declaringClass, name, descriptor)
                else
                    for (callee ← callees) {
                        addCallEdge(caller, pc, callee)
                    }
            }

            override def invokevirtual(
                pc: PC,
                declaringClass: ReferenceType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {

                if (declaringClass.isArrayType) {
                    staticallyBoundInvocation(pc, ObjectType.Object, name, descriptor)
                } else {
                    dynamicallyBoundInvocation(pc, declaringClass.asObjectType, name, descriptor)
                }
                super.invokevirtual(pc, declaringClass, name, descriptor, operands)
            }

            override def invokeinterface(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                dynamicallyBoundInvocation(pc, declaringClass.asObjectType, name, descriptor)
                super.invokeinterface(pc, declaringClass, name, descriptor, operands)
            }

            @inline private[this] def staticallyBoundInvocation(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor) {
                classHierarchy.lookupMethodDefinition(
                    declaringClass, name, descriptor, project
                ) match {
                        case Some(callee) ⇒
                            assume(callee.isNative || callee.body.isDefined,
                                "lookup returned an abstract method")
                            addCallEdge(caller, pc, callee)
                        case None ⇒
                            unresolvedMethodCall(
                                callerClassFile.thisClass, caller, pc,
                                declaringClass, name, descriptor
                            )
                    }
            }

            /**
             * Invocation of private, constructor and super methods.
             */
            override def invokespecial(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                // for invokespecial the dynamic type is not "relevant" and the
                // first method that we find is the one that needs to be concrete 
                staticallyBoundInvocation(pc, declaringClass, name, descriptor)
                super.invokespecial(pc, declaringClass, name, descriptor, operands)
            }

            /**
             * Invocation of static methods.
             */
            override def invokestatic(
                pc: PC,
                declaringClass: ObjectType,
                name: String,
                descriptor: MethodDescriptor,
                operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
                staticallyBoundInvocation(pc, declaringClass, name, descriptor)
                super.invokestatic(pc, declaringClass, name, descriptor, operands)
            }
        }

        while (methodsToAnalyze.nonEmpty) {
            val method = methodsToAnalyze.head
            methodsToAnalyze = methodsToAnalyze.tail
            if (!analyzedMethods(method.id)) {
                analyzedMethods(method.id) = true
                val classFile = theProject.classFile(method)
                BaseAI(classFile, method, new MethodDomain(classFile, method))
            }
        }

        (CHACallGraph(calledBy, calls), unresolvedMethodCalls)
    }
}

class CHACallGraph(

        val calledBy: Map[Method, Map[Method, Set[PC]]],
        val calls: Map[Method, Map[PC, Set[Method]]]) {

}
object CHACallGraph {

    def apply(
        calledBy: Map[Method, Map[Method, Set[PC]]],
        calls: Map[Method, Map[PC, Set[Method]]]) =
        new CHACallGraph(calledBy, calls)
}

/**
 * Represents a method call that could not be resolved. This information is primarily
 * interesting during the development of static analyses.
 *
 * @author Michael Eichberg
 */
case class UnresolvedMethodCall(
        callerClass: ReferenceType,
        caller: Method,
        pc: PC,
        calleeClass: ReferenceType,
        calleeName: String,
        calleeDescriptor: MethodDescriptor) {

    // If we have a very large number of unresolved calls that we want to store, e.g., 
    // in a set we have to make the equals / hashcode computations fast! 

    override def equals(other: Any): Boolean = {
        other match {
            case that: UnresolvedMethodCall ⇒
                (this.callerClass eq that.callerClass) &&
                    (this.caller.id == that.caller.id) &&
                    (this.pc == that.pc) &&
                    (this.calleeClass eq that.calleeClass) &&
                    (this.calleeName eq that.calleeName) &&
                    (this.calleeDescriptor == that.calleeDescriptor)
            case _ ⇒ false
        }
    }

    override val hashCode =
        ((((callerClass.hashCode ^ caller.id) * pc) << 16) ^ calleeName.hashCode)

    override def toString: String = {
        callerClass.toJava+"{ "+Console.BOLD + caller.toJava + Console.RESET+":"+pc+" } => "+
            calleeClass.toJava+"{ "+Console.BOLD + calleeDescriptor.toJava(calleeName) + Console.RESET+" }"
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

 


