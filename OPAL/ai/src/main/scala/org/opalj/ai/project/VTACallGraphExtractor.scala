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
import org.opalj.collection.immutable.UIDSet
import br._
import br.analyses._
import domain._
import domain.l0
import domain.l1
import org.opalj.util.No
import org.opalj.util.Answer
import org.opalj.ai.Domain
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheClassFile
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.ClassHierarchy
import org.opalj.ai.domain.TheCode
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.ClassHierarchy
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC

/**
 * Domain object which is used to calculate the call graph.
 *
 * ==Thread Safety==
 * This domain is not thread-safe. Hence, it can only be used by one abstract interpreter
 * at a time.
 *
 * @author Michael Eichberg
 */
class VTACallGraphExtractor(
    cache: CallGraphCache[MethodSignature, Set[Method]])
        extends CallGraphExtractor {

    protected[this] class AnalysisContext(val domain: TheDomain) {

        val project = domain.project
        val classFile = domain.classFile
        val method = domain.method
        val classHierarchy = domain.project.classHierarchy

        //
        //
        // Managing/Storing Call Edges
        //
        //

        import scala.collection.mutable.OpenHashMap
        import scala.collection.mutable.HashSet

        var unresolvableMethodCalls = List.empty[UnresolvedMethodCall]

        @inline def addUnresolvedMethodCall(
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

        @inline final def addCallEdge(
            pc: PC,
            callees: Set[Method]): Unit = {

            if (callEdgesMap.contains(pc)) {
                callEdgesMap(pc) ++= callees
            } else {
                callEdgesMap.put(pc, callees)
            }
        }

        def allCallEdges: (Method, Map[PC, Set[Method]]) = (method, callEdgesMap)

        def addCallToNullPointerExceptionConstructor(
            callerType: ObjectType, callerMethod: Method, pc: PC) {

            cache.NullPointerExceptionDefaultConstructor match {
                case Some(defaultConstructor) ⇒
                    addCallEdge(pc, HashSet(defaultConstructor))
                case _ ⇒
                    val defaultConstructorDescriptor = MethodDescriptor.NoArgsAndReturnVoid
                    val NullPointerException = ObjectType.NullPointerException
                    addUnresolvedMethodCall(
                        callerType, callerMethod, pc,
                        NullPointerException, "<init>", defaultConstructorDescriptor
                    )
            }
        }

        @inline protected[this] def callees(
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor): Set[Method] = {

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

        def staticCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            operands: domain.Operands) = {

            def handleUnresolvedMethodCall() = {
                addUnresolvedMethodCall(
                    classFile.thisType, method, pc,
                    declaringClassType, name, descriptor
                )
            }

            if (classHierarchy.isKnown(declaringClassType)) {
                classHierarchy.lookupMethodDefinition(
                    declaringClassType, name, descriptor, project) match {
                        case Some(callee) ⇒
                            addCallEdge(pc, HashSet(callee))
                        case None ⇒
                            handleUnresolvedMethodCall()
                    }
            } else {
                handleUnresolvedMethodCall()
            }
        }

        def doNonVirtualCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            receiverIsNull: Answer,
            operands: domain.Operands) {

            def handleUnresolvedMethodCall() {
                addUnresolvedMethodCall(
                    classFile.thisType, method, pc,
                    declaringClassType, name, descriptor
                )
            }

            if (classHierarchy.isKnown(declaringClassType)) {
                classHierarchy.lookupMethodDefinition(
                    declaringClassType, name, descriptor, project) match {
                        case Some(callee) ⇒
                            val callees = HashSet(callee)
                            addCallEdge(pc, callees)
                        case None ⇒
                            handleUnresolvedMethodCall()
                    }
            } else {
                handleUnresolvedMethodCall()
            }
        }

        /**
         * @param receiverMayBeNull The parameter is `false` if:
         *      - a static method is called,
         *      - this is an invokespecial call (in this case the receiver is `this`),
         *      - the receiver is known not to be null and the type is known to be precise.
         */
        def nonVirtualCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            receiverIsNull: Answer,
            operands: domain.Operands) {

            if (receiverIsNull.isYesOrUnknown)
                addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)

            doNonVirtualCall(
                pc,
                declaringClassType, name, descriptor,
                receiverIsNull, operands)
        }

        def doVirtualCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            receiverIsNull: Answer,
            operands: domain.Operands) = {

            val callees: Set[Method] = this.callees(declaringClassType, name, descriptor)
            if (callees.isEmpty) {
                addUnresolvedMethodCall(
                    classFile.thisType, method, pc,
                    declaringClassType, name, descriptor)
            } else {
                addCallEdge(pc, callees)
            }
        }

        def virtualCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            operands: domain.Operands) {
            // MODIFIED CHA - we used the type information that is readily available        
            val receiver =
                domain.typeOfValue(
                    operands(descriptor.parametersCount)
                ).asInstanceOf[IsReferenceValue]
            val receiverIsNull = receiver.isNull

            // Possible Cases:
            //  - the value is precise and has a single type => non-virtual call
            //  - the value is not precise but has an upper type bound that is a subtype 
            //    of the declaringClassType
            //
            //  - the value is null => call to the constructor of NullPointerException
            //  - the value maybe null => additional call to the constructor of NullPointerException
            //
            //  - the value is not precise and the upper type bound is a supertype 
            //    of the declaringClassType => the type hierarchy information is not complete;
            //    the central factory method already "handles" this issue - hence, we don't care 

            // Note that explicitly supporting "MultipleReferencesValues", e.g.,
            // to create a very precise in cases such as:
            //     Object o = null;
            //     if(whatever)
            //       o = new Object();
            //     else
            //       o = new Vector();
            //     o.toString //<----- the relevant call
            // is probably not worth the effort. A simple study of the JDK has
            // shown that in the very vast majority of cases that upper type bound
            // of the value as such is also the upper type bound of a specific value.
            // Hence, the explicit support would not increase the precision.
            // This situation might change if the analysis (as a whole) is getting more
            // precise.

            if (receiverIsNull.isYes) {
                addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)
                return ;
            }

            if (receiverIsNull.isUnknown) {
                addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)
                // ... and continue!
            }

            val upperTypeBound = receiver.upperTypeBound
            if (upperTypeBound.consistsOfOneElement) {
                val theType = upperTypeBound.first
                if (theType.isArrayType)
                    doNonVirtualCall(
                        pc, ObjectType.Object, name, descriptor, receiverIsNull,
                        operands)
                else if (receiver.isPrecise)
                    doNonVirtualCall(
                        pc, theType.asObjectType, name, descriptor, receiverIsNull,
                        operands.asInstanceOf[domain.Operands])
                else {
                    doVirtualCall(
                        pc, theType.asObjectType, name, descriptor, receiverIsNull,
                        operands)
                }
            } else {
                // Recall that the types defining the upper type bound are not in an 
                // inheritance relationship; however, they still may define 
                // the respective method.

                val potentialRuntimeTypes =
                    classHierarchy.directSubtypesOf(upperTypeBound.asInstanceOf[UIDSet[ObjectType]])

                val allCallees =
                    if (potentialRuntimeTypes.nonEmpty) {
                        val potentialRuntimeType = potentialRuntimeTypes.head.asObjectType
                        val callees = this.callees(potentialRuntimeType, name, descriptor)
                        potentialRuntimeTypes.tail.foldLeft(callees) { (r, nextUpperTypeBound) ⇒
                            r ++ this.callees(nextUpperTypeBound.asObjectType, name, descriptor)
                        }
                    } else {
                        Set.empty[Method]
                    }

                if (allCallees.isEmpty) {
                    addUnresolvedMethodCall(
                        classFile.thisType, method, pc,
                        declaringClassType, name, descriptor)
                } else {
                    addCallEdge(pc, allCallees)
                }
            }
        }
    }

    protected def AnalysisContext(domain: TheDomain): AnalysisContext =
        new AnalysisContext(domain)

    def extract(result: AIResult { val domain: TheDomain }): LocalCallGraphInformation = {
        val context = AnalysisContext(result.domain)

        result.domain.code.foreach { (pc, instruction) ⇒
            instruction.opcode match {
                case INVOKEVIRTUAL.opcode ⇒
                    val INVOKEVIRTUAL(declaringClass, name, descriptor) = instruction
                    val operands = result.operandsArray(pc)
                    if (operands != null) {
                        if (declaringClass.isArrayType) {
                            context.nonVirtualCall(
                                pc, ObjectType.Object, name, descriptor,
                                result.domain.refIsNull(
                                    pc, operands(descriptor.parametersCount)),
                                operands.asInstanceOf[context.domain.Operands]
                            )
                        } else {
                            context.virtualCall(
                                pc, declaringClass.asObjectType, name, descriptor,
                                operands.asInstanceOf[context.domain.Operands])
                        }
                    }
                case INVOKEINTERFACE.opcode ⇒
                    val INVOKEINTERFACE(declaringClass, name, descriptor) = instruction
                    val operands = result.operandsArray(pc)
                    if (operands != null) {
                        context.virtualCall(
                            pc, declaringClass, name, descriptor,
                            operands.asInstanceOf[context.domain.Operands])
                    }

                case INVOKESPECIAL.opcode ⇒
                    val INVOKESPECIAL(declaringClass, name, descriptor) = instruction
                    val operands = result.operandsArray(pc)
                    // for invokespecial the dynamic type is not "relevant" (even for Java 8) 
                    if (operands != null) {
                        context.nonVirtualCall(
                            pc, declaringClass, name, descriptor,
                            receiverIsNull = No /*the receiver is "this" object*/ ,
                            operands.asInstanceOf[context.domain.Operands]
                        )
                    }

                case INVOKESTATIC.opcode ⇒
                    val INVOKESTATIC(declaringClass, name, descriptor) = instruction
                    val operands = result.operandsArray(pc)
                    if (operands != null) {
                        context.staticCall(
                            pc, declaringClass, name, descriptor,
                            operands.asInstanceOf[context.domain.Operands])
                    }
                case _ ⇒
                // Nothing to do...
            }
        }

        (context.allCallEdges, context.unresolvableMethodCalls)
    }

}

