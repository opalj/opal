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

import scala.collection.Set
import scala.collection.Map
import scala.collection.mutable.HashSet
import org.opalj.collection.immutable.UIDSet
import br._
import br.analyses._
import domain._
import domain.l0
import domain.l1
import org.opalj.ai.Domain
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheClassFile
import org.opalj.ai.domain.TheMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.VirtualMethodInvocationInstruction
import scala.collection.mutable.HashMap

/**
 * The `VTACallGraphExtractor` extracts call edges using the type information at hand.
 * I.e., it does not use the specified declaring class type, but instead uses the
 * type information about the receiver value that are available.
 *
 * @author Michael Eichberg
 */
class VTACallGraphExtractor[TheDomain <: Domain with TheProject with TheClassFile with TheMethod](
    val cache: CallGraphCache[MethodSignature, Set[Method]],
    Domain: (ClassFile, Method) ⇒ TheDomain)
        extends CallGraphExtractor {

    protected[this] class AnalysisContext(val domain: TheDomain)
            extends super.AnalysisContext {

        val project = domain.project
        val classHierarchy = project.classHierarchy
        val classFile = domain.classFile
        val method = domain.method

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

        /**
         * @note The receiver is either not null or it is unknown whether the receiver
         *      is null. However, appropriate edges are already added to the call graph.
         */
        def doNonVirtualCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            operands: domain.Operands): Unit = {

            def handleUnresolvedMethodCall(): Unit = {
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
            operands: domain.Operands): Unit = {

            if (receiverIsNull.isYesOrUnknown)
                addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)

            doNonVirtualCall(
                pc,
                declaringClassType, name, descriptor,
                //receiverIsNull,
                operands)
        }

        def doVirtualCall(
            pc: PC,
            declaringClassType: ObjectType,
            name: String,
            descriptor: MethodDescriptor,
            operands: domain.Operands): Unit = {

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
            operands: domain.Operands): Unit = {
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

            if (receiverIsNull.isYes) {
                addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)
                return ;
            }

            if (receiverIsNull.isUnknown) {
                addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)
                // ... and continue!
            }

            @inline def handleVirtualNonNullCall(
                upperTypeBound: UpperTypeBound,
                receiverIsPrecise: Boolean): Unit = {

                assert(upperTypeBound.nonEmpty)

                if (upperTypeBound.isSingletonSet) {
                    val theType = upperTypeBound.first
                    if (theType.isArrayType)
                        doNonVirtualCall(
                            pc, ObjectType.Object, name, descriptor,
                            //receiverIsNull,
                            operands)
                    else if (receiverIsPrecise)
                        doNonVirtualCall(
                            pc, theType.asObjectType, name, descriptor,
                            //receiverIsNull,
                            operands.asInstanceOf[domain.Operands])
                    else {
                        doVirtualCall(
                            pc, theType.asObjectType, name, descriptor,
                            //receiverIsNull,
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
                        // Fallback to ensure that the call graph does not miss an
                        // edge; it may be the case that the (unknown) subtypes actually
                        // just inherit one of the methods of the (known) supertype.
                        doVirtualCall(
                            pc, declaringClassType, name, descriptor,
                            ///receiverIsNull,
                            operands)
                    } else {
                        addCallEdge(pc, allCallees)
                    }
                }
            }

            val receiverUpperTypeBound = receiver.upperTypeBound

            val receivers = receiver.referenceValues
            if (receivers.tail.nonEmpty) {
                // the reference value is a "MultipleReferenceValue"

                // The following numbers are created using ExtVTA for JDK 1.8.0_25
                // and refer to a call graph created without explicit support for
                // multiple reference values:
                //     Creating the call graph took: ~28sec (Mac Pro; 3 GHz 8-Core Intel Xeon E5)
                //     Number of call sites: 911.253
                //     Number of call edges: 6.925.997
                //
                // With explicit support, we get the following numbers:
                //     Number of call sites: 911.253
                //     Number of call edges: 6.923.015

                val receiversAreMorePrecise =
                    !receiver.isPrecise &&
                        // the receiver as a whole is not precise...
                        receivers.forall { aReceiver ⇒
                            val anUpperTypeBound = aReceiver.upperTypeBound
                            aReceiver.isPrecise || {
                                anUpperTypeBound != receiverUpperTypeBound &&
                                    classHierarchy.isSubtypeOf(anUpperTypeBound, receiverUpperTypeBound).isYes
                            }
                        }
                if (receiversAreMorePrecise) {
                    // THERE IS POTENTIAL FOR A MORE PRECISE CALL GRAPH SIMPLY
                    // BECAUSE OF THE TYPE INFORMATION!
                    val uniqueReceivers =
                        receivers.foldLeft(Map.empty[UpperTypeBound, Boolean]) { (results, rv) ⇒
                            val utb = rv.upperTypeBound
                            if (utb.nonEmpty)
                                results.get(utb) match {
                                    case Some(isPrecise) ⇒
                                        if (isPrecise && !rv.isPrecise) {
                                            results.updated(utb, false)
                                        } else {
                                            results
                                        }
                                    case None ⇒
                                        results + ((utb, rv.isPrecise))
                                }
                            else
                                // empty upper type bounds (those of null values) are
                                // already handled
                                results
                        }
                    uniqueReceivers.foreach { rv ⇒
                        val (utb, isPrecise) = rv
                        handleVirtualNonNullCall(utb, isPrecise)
                    }
                } else {
                    // we did not get anything from analyzing the "MultipleReferenceValue"
                    // let's continue with the default handling
                    handleVirtualNonNullCall(receiverUpperTypeBound, receiver.isPrecise)
                }
            } else {
                // the value is not a "MultipleReferenceValue"
                handleVirtualNonNullCall(receiverUpperTypeBound, receiver.isPrecise)
            }
        }
    }

    protected def AnalysisContext(domain: TheDomain): AnalysisContext =
        new AnalysisContext(domain)

    private[this] val chaCallGraphExctractor =
        new CHACallGraphExtractor(cache /*it should not be used...*/ )

    def extract(
        project: SomeProject,
        classFile: ClassFile,
        method: Method): CallGraphExtractor.LocalCallGraphInformation = {

        // The following optimization (using the plain CHA algorithm for all methods
        // that do not virtual method calls) may lead to some additional edges (if
        // the underlying code contains dead code), but the improvement is worth the
        // very few additional edges due to statically identifiable dead code!
        val hasVirtualMethodCalls =
            method.body.get.instructions.exists { i ⇒
                i.isInstanceOf[VirtualMethodInvocationInstruction]
            }
        if (!hasVirtualMethodCalls)
            return chaCallGraphExctractor.extract(project, classFile, method)

        // There are virtual calls, hence, we now do the call graph extraction using
        // variable type analysis

        val result = BaseAI(classFile, method, Domain(classFile, method))
        val context = AnalysisContext(result.domain)

        result.domain.code.foreach { (pc, instruction) ⇒
            (instruction.opcode: @scala.annotation.switch) match {
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

