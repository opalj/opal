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
import org.opalj.ai.Domain
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheClassFile
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.ClassHierarchy
import org.opalj.ai.domain.TheCode
import org.opalj.br.analyses.SomeProject
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
class VTACallGraphExtractor(
    cache: CallGraphCache[MethodSignature, Set[Method]])
        extends CHACallGraphExtractor(cache) {

    protected[this] class AnalysisContext(override val domain: TheDomain)
            extends super.AnalysisContext(domain) {

        override def virtualCall(
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
            /* 
            // CODE TO EVALUATE THE USEFULLNESS OF EXPLICITLY SUPPORTING 
            // MULTIPLEREFERENCEVALUES
            if (receiver.referenceValues.tail.nonEmpty) {
                val commonUpperTypeBound = receiver.upperTypeBound.toString()
                println("Common upper type bound: "+commonUpperTypeBound +
                    receiver.referenceValues.mkString(" { Values: ", ",", " }\n\n"))
            }
            */

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

    override protected def AnalysisContext(domain: TheDomain): AnalysisContext =
        new AnalysisContext(domain)

}

