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
import collection.mutable.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Domain object which is used to calculate the call graph.
 *
 * ==Thread Safety==
 * This domain is thread-safe. (The used builder has to be thread-safe, too!)
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
    /* abstract */ val cache: CHACache

    /* abstract */ def unresolvedMethodCall(
        callerClass: ReferenceType, caller: Method, pc: PC,
        calleeClass: ReferenceType, calleeName: String, calleeDescriptor: MethodDescriptor): Unit

    //
    // IMPLEMENTATION
    //

    private[this] var _callEdges = List.empty[(Method, PC, Iterable[Method])]

    @inline final def addCallEdge(
        caller: Method,
        pc: PC,
        callees: Iterable[Method]): Unit = {
        _callEdges = (caller, pc, callees) :: _callEdges
    }

    def callEdges: List[(Method, PC, Iterable[Method])] = _callEdges

    @inline private[this] def dynamicallyBoundInvocation(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor) {
        // PLAIN CHA - we do not consider any data-flow information 
        // that is available

        val callees = cache.getOrElseUpdate(
            declaringClass, new MethodSignature(name, descriptor),
            classHierarchy.lookupImplementingMethods(
                declaringClass, name, descriptor, project))

        if (callees.isEmpty)
            unresolvedMethodCall(
                callerClassFile.thisClass, caller, pc,
                declaringClass, name, descriptor)
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
            staticallyBoundInvocation(pc, ObjectType.Object, name, descriptor)
        } else {
            dynamicallyBoundInvocation(pc, declaringClass.asObjectType, name, descriptor)
        }
        super.invokevirtual(pc, declaringClass, name, descriptor, operands)
    }

    abstract override def invokeinterface(
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
                    addCallEdge(caller, pc, Iterable(callee))
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
    abstract override def invokespecial(
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
    abstract override def invokestatic(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): OptionalReturnValueOrExceptions = {
        staticallyBoundInvocation(pc, declaringClass, name, descriptor)
        super.invokestatic(pc, declaringClass, name, descriptor, operands)
    }
}

class CHACache private (
        private[this] val cache: Array[HashMap[MethodSignature, Iterable[Method]]]) {

    private[this] val cacheMutexes: Array[Object] = {
        val a = new Array(cache.size)
        Array.fill(cache.size)(new Object)
    }

    def getOrElseUpdate(
        declaringClass: ReferenceType,
        callerSignature: MethodSignature,
        orElse: ⇒ Iterable[Method]): Iterable[Method] = {

        @inline def updateCachedResults(
            resolvedTargetsForClass: HashMap[MethodSignature, Iterable[Method]]) = {
            resolvedTargetsForClass.synchronized {
                resolvedTargetsForClass.getOrElseUpdate(
                    callerSignature,
                    orElse
                )
            }
        }

        val id = declaringClass.id
        val cachedResults = {
            val cachedResults = { cacheMutexes(id).synchronized { cache(id) } }
            if (cachedResults eq null) {
                cacheMutexes(id).synchronized {
                    val cachedResults = cache(declaringClass.id)
                    if (cachedResults eq null) { // still eq null... 
                        val targets = orElse
                        cache(declaringClass.id) = HashMap((callerSignature, targets))
                        return targets
                    } else {
                        cachedResults
                    }
                }
            } else {
                cachedResults
            }
        }
        updateCachedResults(cachedResults)
    }
}
object CHACache {

    def apply(project: SomeProject): CHACache = {
        new CHACache(new Array(project.objectTypesCount))
    }
}

/**
 * Domain object which is used to calculate the call graph.
 */
class DefaultCHACallGraphDomain[Source](
    val project: Project[Source],
    val cache: CHACache,
    val callerClassFile: ClassFile,
    val caller: Method,
    private[this] val _unresolvedMethodCall: (ReferenceType, Method, PC, ReferenceType, String, MethodDescriptor) ⇒ Unit)
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

    def unresolvedMethodCall(
        callerClass: ReferenceType,
        caller: Method,
        pc: PC,
        calleeClass: ReferenceType,
        calleeName: String,
        calleeDescriptor: MethodDescriptor): Unit = {
        _unresolvedMethodCall(callerClass, caller, pc, calleeClass, calleeName, calleeDescriptor)
    }

}




