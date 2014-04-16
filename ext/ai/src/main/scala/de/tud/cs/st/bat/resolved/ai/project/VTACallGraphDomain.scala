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
trait VTACallGraphDomain[Source, I] extends CHACallGraphDomain[Source, I] { domain ⇒

    @inline override protected[this] def virtualMethodCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): Unit = {
        // MODIFIED CHA - we used the type information that is readily available
        val receiver = operands.last
        val IsAReferenceValue(value) = typeOfValue(receiver)

        // Possible Cases:
        //  - the value is precise and has a single type => static call
        //  - the value is not precise but has an upper type bound that is a subtype of the declaringClassType. 
        //  - the value is not precise and the upper type bound is a supertype 
        //    of the declaringClassType => "strange" nevertheless, treated as a 
        //    standard virtual call with the upper type bound set to the declaring class.
        //  - the value is null => call to the constructor of NullPointerException
        //  - the value maybe null => additional call to the constructor of NullPointerException

        val isNull = value.isNull
        if (isNull.isYesOrUnknown) {
            staticMethodCall(
                pc,
                ObjectType.NullPointerException,
                "<init>",
                MethodDescriptor.NoArgsAndReturnVoid,
                List(domain.NullPointerException(pc)))
        }

        // there may be additional calls
        if (isNull.isNoOrUnknown) {
            val isPrecise = value.isPrecise
            val upperTypeBound = value.upperTypeBound
            if (isPrecise && upperTypeBound.tail.isEmpty) {
                val theType = upperTypeBound.head
                if (theType.isArrayType)
                    staticMethodCall(pc, ObjectType.Object, name, descriptor, operands)
                else
                    staticMethodCall(pc, theType.asObjectType, name, descriptor, operands)
            } else {
                // _Also_ supports the case where we have a "precise type", but
                // multiple types as an upper bound. This is useful in some selected
                // cases where the class is generated dynamically at runtime and 
                // hence, the currently available information is simply the best that
                // is available.

                for (utb ← upperTypeBound) {
                    if (utb.isArrayType) {
                        staticMethodCall(pc, ObjectType.Object, name, descriptor, operands)
                    } else if (domain.isSubtypeOf(declaringClassType, utb).isYes) {
                        // for whatever reason, but the invoke's declaring class type
                        // is "more" precise
                        super.virtualMethodCall(pc, declaringClassType, name, descriptor, operands)
                        return // it doesn't make sense
                    } else {
                        super.virtualMethodCall(pc, utb.asObjectType, name, descriptor, operands)
                    }
                }
            }
        }
    }
}
/**
 * Domain object which is used to calculate the call graph using variable type analysis.
 *
 * @author Michael Eichberg
 */
class DefaultVTACallGraphDomain[Source](
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
        with l1.DefaultReferenceValuesBinding[Int]
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with ProjectBasedClassHierarchy[Source]
        with VTACallGraphDomain[Source, Int] {

    def identifier = theMethod.id

}




