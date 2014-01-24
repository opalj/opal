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
import analyses._

import scala.collection.Set
import scala.collection.Map

/**
 * Domain object which is used to calculate the call graph.
 *
 * ==Thread Safety==
 * This domain is not thread-safe. Hence, it can only be used by one abstract interpreter
 * at a time.
 *
 * @author Michael Eichberg
 */
trait VTACallGraphDomain[Source, I] extends CHACallGraphDomain[Source, I] {

    //    @inline private[this] def virtualMethodCall(
    //        pc: PC,
    //        declaringClassType: ReferenceType,
    //        name: String,
    //        descriptor: MethodDescriptor,
    //        operands: List[DomainValue]) {
    //        // MODIFIED CHA - we used the type information that is readily available
    //
    //        @inline def lookupImplementingMethods(receiverClassType: ObjectType) {
    //            val callees = cache.getOrElseUpdate(
    //                declaringClassType, new MethodSignature(name, descriptor),
    //                classHierarchy.lookupImplementingMethods(
    //                    receiverClassType, name, descriptor, project))
    //
    //            if (callees.isEmpty)
    //                addUnresolvedMethodCall(
    //                    callerclassFile.thisType, caller, pc,
    //                    declaringClassType, name, descriptor)
    //            else {
    //                addCallEdge(caller, pc, callees)
    //            }
    //        }
    //
    //        //        val IsReferenceValue(receiver) = typeOfValue(operands.last)
    //        //        if (receiver.size == 1 && receiver.head.upperBound.size == 1) {
    //        //            // Here, we could do much more... (e.g., calculate a single common upper type 
    //        //            // and use that for the lookup...)
    //        //            val valueBasedUpperBound = receiver.head
    //        //            val receiverType = valueBasedUpperBound.upperBound.head
    //        //            if (receiverType.isArrayType) {
    //        //                fixedMethodCall(pc, ObjectType.Object, name, descriptor)
    //        //            } else if (valueBasedUpperBound.isPrecise) {
    //        //                fixedMethodCall(
    //        //                    pc,
    //        //                    receiverType.asObjectType,
    //        //                    name,
    //        //                    descriptor)
    //        //            } else {
    //        //                val classType = receiverType.asObjectType
    //        //                if (isSubtypeOf(classType, declaringClassType).yes)
    //        //                    lookupImplementingMethods(classType)
    //        //                else
    //        //                    lookupImplementingMethods(declaringClassType.asObjectType)
    //        //            }
    //        //        } else {
    //        lookupImplementingMethods(declaringClassType.asObjectType)
    //        //        }
    //    }

}
///**
// * Domain object which is used to calculate the call graph.
// */
//class DefaultVTACallGraphDomain[Source](
//    val project: Project[Source],
//    val cache: CallGraphCache[MethodSignature, Iterable[Method]],
//    val theClassFile: ClassFile,
//    val theMethod: Method)
//        extends Domain[Int]
//        with Configuration
//        with DefaultDomainValueBinding[Int]
//        with IgnoreMethodResults
//        with IgnoreSynchronization
//        with l0.DefaultTypeLevelIntegerValues[Int]
//        with l0.DefaultTypeLevelLongValues[Int]
//        with l0.DefaultTypeLevelFloatValues[Int]
//        with l0.DefaultTypeLevelDoubleValues[Int]
//        with l1.BaseReferenceValuesBinding[Int]
//        //with l1.StringValues[Int]
//        with l0.TypeLevelFieldAccessInstructions
//        with l0.TypeLevelInvokeInstructions
//        with l1.ProjectBasedClassHierarchy[Source]
//        with VTACallGraphDomain[Source, Int] {
//
//    def identifier = theMethod.id
//
//}




