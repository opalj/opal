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

import org.opalj.ai.domain.ClassHierarchy

/**
 * Domain object which is used to calculate the call graph.
 *
 * ==Thread Safety==
 * This domain is not thread-safe. Hence, it can only be used by one abstract interpreter
 * at a time.
 *
 * @author Michael Eichberg
 */
trait VTACallGraphDomain extends CHACallGraphDomain {
    domain: MethodCallsHandling with ReferenceValuesDomain with TypedValuesFactory with Configuration with TheProject with ClassHierarchy with TheMethod with TheCode ⇒

    @inline override protected[this] def virtualCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {
        // MODIFIED CHA - we used the type information that is readily available
        val receiver = typeOfValue(operands.last).asInstanceOf[IsReferenceValue]
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
            return handleInstanceBasedInvoke(pc, descriptor, receiverIsNull);
        }

        if (receiverIsNull.isUnknown) {
            addCallToNullPointerExceptionConstructor(classFile.thisType, method, pc)
            // ... and continue!
        }

        val upperTypeBound = receiver.upperTypeBound
        if (upperTypeBound.consistsOfOneElement) {
            val theType = upperTypeBound.first
            if (theType.isArrayType)
                doNonVirtualCall(pc, ObjectType.Object, name, descriptor, receiverIsNull, operands)
            else if (receiver.isPrecise)
                doNonVirtualCall(pc, theType.asObjectType, name, descriptor, receiverIsNull, operands)
            else {
                doVirtualCall(pc, theType.asObjectType, name, descriptor, receiverIsNull, operands)
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
                handleInstanceBasedInvoke(pc, descriptor, receiverIsNull)
            } else {
                addCallEdge(pc, allCallees)
                handleInstanceBasedInvoke(pc, descriptor, allCallees, receiverIsNull, operands)
            }

            //                val receiverType =
            //                    classHierarchy.joinObjectTypesUntilSingleUpperBound(
            //                        // the following cast is safe, because we cannot have
            //                        // an UpperTypeBound with more than one value that
            //                        // contains ArrayTypes
            //                        upperTypeBound.asInstanceOf[UIDSet[ObjectType]],
            //                        true)
            //                doVirtualCall(pc, receiverType, name, descriptor, receiverIsNull, operands)

            //                // _Also_ supports the case where we have a "precise type" with
            //                // multiple types as an upper bound. This is useful in some selected
            //                // cases where the class is generated dynamically at runtime and 
            //                // hence, the currently available information is simply the best that
            //                // is available.           
            //                for (utb ← upperTypeBound) {
            //                    if ((declaringClassType ne utb) &&
            //                        domain.isSubtypeOf(declaringClassType, utb).isYes) {
            //                        // The invoke's declaring class type is "more" precise
            //                        println(
            //                            Console.YELLOW+"[warn] type information missing: "+
            //                                utb.toJava+"(underlying value="+receiver+")"+
            //                                " part of the upper type bound "+
            //                                upperTypeBound.map(_.toJava).mkString("(", ",", ")")+
            //                                " should be a subtype of the type of the method's declaring class: "+
            //                                declaringClassType.toJava+
            //                                " (but this cannot be deduced reliably from the project)"+Console.RESET)
            //                        doVirtualCall(pc, declaringClassType, name, descriptor, receiverIsNull, operands)
            //                    } else {
            //                        val callees = this.callees(utb.asObjectType, name, descriptor)
            //                        callees.filter { m ⇒
            //                            upperTypeBound.exists(
            //                                domain.isSubtypeOf(project.classFile(m).thisType, _).isYesOrUnknown
            //                            )
            //                        }
            //                    }
            //                }

        }
    }
}

