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

import domain._
import domain.l0
import domain.l1

import analyses.Project

import scala.collection.Set
import scala.collection.Map

/**
 * Domain object that can be used to calculate a call graph using CHA. This domain
 * basically collects for all invoke instructions of a method the potential target
 * methods that may be invoked at runtime.
 *
 * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to the
 * respective methods of `java.lang.Object`.
 *
 * Signature polymorphic methods are correctly resolved (done by the
 * `lookupImplementingMethod` method defined in `ClassHierarchy`.)
 *
 * ==Thread Safety==
 * '''This domain is not thread-safe'''. Hence, it can only be used by one abstract interpreter
 * at a time. However, it is no problem to have multiple abstract interpreters that
 * process different methods where each uses its own instance of a CHACallGraphDomain
 * object.
 *
 * @author Michael Eichberg
 */
trait CHACallGraphDomain[Source, I]
        extends CallGraphDomain[Source, I]
        with ClassHierarchy {

    //
    // Helper data structures  
    //
    /* abstract */ val cache: CallGraphCache[MethodSignature, Iterable[Method]]

    //
    // IMPLEMENTATION
    //

    private[this] var unresolvedMethodCalls = List.empty[UnresolvedMethodCall]

    @inline final private[this] def addUnresolvedMethodCall(
        callerClass: ReferenceType, caller: Method, pc: PC,
        calleeClass: ReferenceType, calleeName: String, calleeDescriptor: MethodDescriptor): Unit = {
        unresolvedMethodCalls =
            new UnresolvedMethodCall(
                callerClass, caller, pc,
                calleeClass, calleeName, calleeDescriptor
            ) :: unresolvedMethodCalls
    }

    def allUnresolvedMethodCalls: List[UnresolvedMethodCall] = unresolvedMethodCalls

    private[this] var callEdges = List.empty[(Method, PC, Iterable[Method])]

    @inline final private[this] def addCallEdge(
        pc: PC,
        callees: Iterable[Method]): Unit = {
        callEdges = (theMethod, pc, callees) :: callEdges
    }

    def allCallEdges: List[(Method, PC, Iterable[Method])] = callEdges

    @inline private[this] def staticMethodCall(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor) {
        classHierarchy.lookupMethodDefinition(
            declaringClass, name, descriptor, project
        ) match {
                case Some(callee) ⇒
                    addCallEdge(pc, Iterable(callee))
                case None ⇒
                    addUnresolvedMethodCall(
                        theClassFile.thisType, theMethod, pc,
                        declaringClass, name, descriptor
                    )
            }
    }

    @inline private[this] def virtualMethodCall(
        pc: PC,
        declaringClassType: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]) {

        val callees = cache.getOrElseUpdate(
            declaringClassType, new MethodSignature(name, descriptor),
            classHierarchy.lookupImplementingMethods(
                declaringClassType.asObjectType, name, descriptor, project))

        if (callees.isEmpty)
            addUnresolvedMethodCall(
                theClassFile.thisType, theMethod, pc,
                declaringClassType, name, descriptor)
        else {
            addCallEdge(pc, callees)
        }
    }

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {
        if (declaringClass.isArrayType) {
            staticMethodCall(pc, ObjectType.Object, name, descriptor)
        } else {
            virtualMethodCall(pc, declaringClass, name, descriptor, operands)
        }
        super.invokevirtual(pc, declaringClass, name, descriptor, operands)
    }

    abstract override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {
        virtualMethodCall(pc, declaringClass, name, descriptor, operands)
        super.invokeinterface(pc, declaringClass, name, descriptor, operands)
    }

    /**
     * Invocation of private, constructor and super methods.
     */
    abstract override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {
        // for invokespecial the dynamic type is not "relevant" and the
        // first method that we find is the one that needs to be concrete 
        staticMethodCall(pc, declaringClass, name, descriptor)
        super.invokespecial(pc, declaringClass, name, descriptor, operands)
    }

    /**
     * Invocation of static methods.
     */
    abstract override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): MethodCallResult = {
        staticMethodCall(pc, declaringClass, name, descriptor)
        super.invokestatic(pc, declaringClass, name, descriptor, operands)
    }
}

/**
 * Domain object which is used to calculate the call graph.
 */
class DefaultCHACallGraphDomain[Source](
    val project: Project[Source],
    val cache: CallGraphCache[MethodSignature, Iterable[Method]],
    val theClassFile: ClassFile,
    val theMethod: Method)
        extends Domain[Int]
        with DefaultDomainValueBinding[Int]
        with GeneralizedArrayHandling
        with Configuration
        with IgnoreMethodResults
        with IgnoreSynchronization
        with l0.DefaultTypeLevelIntegerValues[Int]
        with l0.DefaultTypeLevelLongValues[Int]
        with l0.DefaultTypeLevelFloatValues[Int]
        with l0.DefaultTypeLevelDoubleValues[Int]
        with l0.BaseReferenceValuesBinding[Int]       
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with l1.ProjectBasedClassHierarchy[Source]
        with CHACallGraphDomain[Source, Int] {

    def identifier = theMethod.id

}




