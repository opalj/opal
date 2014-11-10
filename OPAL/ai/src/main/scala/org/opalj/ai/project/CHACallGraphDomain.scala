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
package project

import scala.collection.Set
import scala.collection.Map

import br._

import domain._

/**
 * Domain object that can be used to calculate a call graph using CHA. This domain
 * basically collects – for all invoke instructions of a method – the potential target
 * methods that may be invoked at runtime.
 *
 * Virtual calls on Arrays (clone(), toString(),...) are replaced by calls to the
 * respective methods of `java.lang.Object`.
 *
 * Signature polymorphic methods are correctly resolved (done by the method
 * `lookupImplementingMethod` defined in `ClassHierarchy`.)
 *
 * ==Thread Safety==
 * '''This domain is not thread-safe'''. However, given the strong coupling of a
 * domain instance to a specific method this is usually not an issue.
 *
 * @author Michael Eichberg
 */
trait CHACallGraphDomain extends CallGraphDomain {
    domain: TheProject with TheMethod with ClassHierarchy ⇒

    //
    // Helper data structures (shared across all instances of this domain) 
    //
    /* abstract */ val cache: CallGraphCache[MethodSignature, Set[Method]]

    //
    //
    // Managing/Storing Call Edges
    //
    //

    import scala.collection.mutable.OpenHashMap
    import scala.collection.mutable.HashSet

    private[this] var unresolvableMethodCalls = List.empty[UnresolvedMethodCall]

    @inline final private[this] def addUnresolvedMethodCall(
        callerClass: ReferenceType, caller: Method, pc: PC,
        calleeClass: ReferenceType, calleeName: String, calleeDescriptor: MethodDescriptor): Unit = {
        unresolvableMethodCalls =
            new UnresolvedMethodCall(
                callerClass, caller, pc,
                calleeClass, calleeName, calleeDescriptor
            ) :: unresolvableMethodCalls
    }

    def allUnresolvableMethodCalls: List[UnresolvedMethodCall] = unresolvableMethodCalls

    private[this] val callEdgesMap = OpenHashMap.empty[PC, Set[Method]]

    @inline final private[this] def addCallEdge(
        pc: PC,
        callees: Set[Method]): Unit = {

        if (callEdgesMap.contains(pc)) {
            callEdgesMap(pc) ++= callees
        } else {
            callEdgesMap.put(pc, callees)
        }
    }

    def allCallEdges: (Method, Map[PC, Set[Method]]) = (method, callEdgesMap)

    //
    //
    // Logic to derive call edges
    //
    //

    def NullPointerExceptionConstructorCall(
        callerType: ObjectType, callerMethod: Method, pc: PC) {

        cache.NullPointerExceptionDefaultConstructor match {
            case Some(defaultConstructor) ⇒
                addCallEdge(pc, HashSet(defaultConstructor))
            case _ ⇒
                addUnresolvedMethodCall(
                    callerType, callerMethod, pc,
                    ObjectType("java/lang/NullPointerException"), "<init>", MethodDescriptor.NoArgsAndReturnVoid
                )
        }
    }

    @inline protected[this] def doNonVirtualCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): Unit = {

        def handleUnresolvedMethodCall() =
            addUnresolvedMethodCall(
                classFile.thisType, method, pc,
                declaringClassType, name, descriptor
            )

        if (classHierarchy.isKnown(declaringClassType)) {
            classHierarchy.lookupMethodDefinition(
                declaringClassType, name, descriptor, project
            ) match {
                    case Some(callee) ⇒
                        addCallEdge(pc, HashSet(callee))
                    case None ⇒
                        handleUnresolvedMethodCall()
                }
        } else {
            handleUnresolvedMethodCall()
        }
    }

    @inline protected[this] def nonVirtualCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        receiverMayBeNull: Boolean,
        operands: Operands): Unit = {

        if (receiverMayBeNull)
            NullPointerExceptionConstructorCall(classFile.thisType, method, pc)

        doNonVirtualCall(pc, declaringClassType, name, descriptor, operands)
    }

    @inline protected[this] def callees(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): Set[Method] = {

        if (classHierarchy.isKnown(declaringClassType)) {
            val methodSignature = new MethodSignature(name, descriptor)
            cache.getOrElseUpdate(declaringClassType, methodSignature) {
                classHierarchy.lookupImplementingMethods(
                    declaringClassType, name, descriptor, project
                )
            }
        } else {
            Set.empty
        }
    }

    @inline protected[this] def doVirtualCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): Unit = {

        val callees: Set[Method] =
            this.callees(pc, declaringClassType, name, descriptor, operands)

        if (callees.isEmpty) {
            addUnresolvedMethodCall(
                classFile.thisType, method, pc,
                declaringClassType, name, descriptor)
        } else {
            addCallEdge(pc, callees)
        }
    }

    /**
     * @note A virtual method call is always an instance based call and never a call to
     *      a static method.
     */
    @inline protected[this] def virtualCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): Unit = {

        NullPointerExceptionConstructorCall(classFile.thisType, method, pc)

        doVirtualCall(pc, declaringClassType, name, descriptor, operands)
    }

    //
    //
    // Implementation of the standard "invokeXYZ" instructions
    //
    //

    abstract override def invokevirtual(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        if (declaringClass.isArrayType) {
            nonVirtualCall(
                pc, ObjectType.Object, name, descriptor,
                receiverMayBeNull = true, operands
            )
        } else {
            virtualCall(pc, declaringClass.asObjectType, name, descriptor, operands)
        }

        super.invokevirtual(pc, declaringClass, name, descriptor, operands)
    }

    abstract override def invokeinterface(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        virtualCall(pc, declaringClass, name, descriptor, operands)

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
        operands: Operands): MethodCallResult = {

        // for invokespecial the dynamic type is not "relevant" (even for Java 8) 
        nonVirtualCall(
            pc, declaringClass, name, descriptor,
            receiverMayBeNull = false /*the receiver is "this" object*/ , operands
        )

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
        operands: Operands): MethodCallResult = {

        nonVirtualCall(
            pc, declaringClass, name, descriptor,
            receiverMayBeNull = false /*the receiver is the "class" object*/ , operands)

        super.invokestatic(pc, declaringClass, name, descriptor, operands)
    }
}

