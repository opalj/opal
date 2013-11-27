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
import bat.resolved.analyses._
import collection.Set
import collection.Map

/**
 * Domain object which is used to calculate the call graph.
 *
 * ==Thread Safety==
 * This domain is not thread-safe. Hence, it can only be used by one abstract interpreter
 * at a time.
 *
 * @author Michael Eichberg
 */
trait CHACallGraphDomain[Source, I]
        extends Domain[I]
        with ClassHierarchyDomain {

    /* abstract */ val project: Project[Source]
    //
    // The method we want to analyze
    //
    /* abstract */ def callerClassFile: ClassFile

    /* abstract */ def caller: Method

    //
    // Helper data structures  
    //
    /* abstract */ val cache: CallGraphCache[MethodSignature, Iterable[Method]]

    //
    // IMPLEMENTATION
    //

    private[this] var _unresolvedMethodCalls = List.empty[UnresolvedMethodCall]

    @inline final private[this] def addUnresolvedMethodCall(
        callerClass: ReferenceType, caller: Method, pc: PC,
        calleeClass: ReferenceType, calleeName: String, calleeDescriptor: MethodDescriptor): Unit = {
        _unresolvedMethodCalls =
            new UnresolvedMethodCall(
                callerClass, caller, pc,
                calleeClass, calleeName, calleeDescriptor
            ) :: _unresolvedMethodCalls
    }

    def unresolvedMethodCalls: List[UnresolvedMethodCall] = _unresolvedMethodCalls

    private[this] var _callEdges = List.empty[(Method, PC, Iterable[Method])]

    @inline final private[this] def addCallEdge(
        caller: Method,
        pc: PC,
        callees: Iterable[Method]): Unit = {
        _callEdges = (caller, pc, callees) :: _callEdges
    }

    def callEdges: List[(Method, PC, Iterable[Method])] = _callEdges

    @inline private[this] def fixedMethodCall(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor) {
        classHierarchy.lookupMethodDefinition(
            declaringClass, name, descriptor, project
        ) match {
                case Some(callee) ⇒
                    addCallEdge(caller, pc, Iterable(callee))
                case None ⇒
                    addUnresolvedMethodCall(
                        callerClassFile.thisClass, caller, pc,
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
        // MODIFIED CHA - we used the type information that is readily available

        val callees = cache.getOrElseUpdate(
            declaringClassType, new MethodSignature(name, descriptor),
            classHierarchy.lookupImplementingMethods(
                declaringClassType.asObjectType, name, descriptor, project))

        if (callees.isEmpty)
            addUnresolvedMethodCall(
                callerClassFile.thisClass, caller, pc,
                declaringClassType, name, descriptor)
        else {
            addCallEdge(caller, pc, callees)
        }
    }

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
        if (declaringClass.isArrayType) {
            fixedMethodCall(pc, ObjectType.Object, name, descriptor)
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
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
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
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
        // for invokespecial the dynamic type is not "relevant" and the
        // first method that we find is the one that needs to be concrete 
        fixedMethodCall(pc, declaringClass, name, descriptor)
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
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
        fixedMethodCall(pc, declaringClass, name, descriptor)
        super.invokestatic(pc, declaringClass, name, descriptor, operands)
    }
}
/**
 * Domain object which is used to calculate the call graph.
 */
class DefaultCHACallGraphDomain[Source](
    val project: Project[Source],
    val cache: CallGraphCache[MethodSignature, Iterable[Method]],
    val callerClassFile: ClassFile,
    val caller: Method)
        extends Domain[Int]
        with DefaultDomainValueBinding[Int]
        with DefaultTypeLevelIntegerValues[Int]
        with DefaultTypeLevelLongValues[Int]
        with DefaultTypeLevelFloatValues[Int]
        with DefaultTypeLevelDoubleValues[Int]
        with DefaultTypeLevelReferenceValues[Int]
        //with DefaultPreciseReferenceValues[Int]
        //with StringValues[Int]
        with TypeLevelArrayInstructions
        with TypeLevelFieldAccessInstructions
        with TypeLevelInvokeInstructions
        with DoNothingOnReturnFromMethod
        with ProjectBasedClassHierarchy[Source]
        with CHACallGraphDomain[Source, Int] {

    def identifier = caller.id

}




