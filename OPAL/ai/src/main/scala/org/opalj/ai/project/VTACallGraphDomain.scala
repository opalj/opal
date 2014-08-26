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
    domain: TheProject[_] with TheMethod with ClassHierarchy ⇒

    @inline override protected[this] def unresolvedCall(
        pc: PC,
        declaringClassType: ObjectType,
        name: String,
        descriptor: MethodDescriptor,
        operands: Operands): Unit = {
        // MODIFIED CHA - we used the type information that is readily available
        val receiver = operands.last
        val value = typeOfValue(receiver).asInstanceOf[IsAReferenceValue]

        // Possible Cases:
        //  - the value is precise and has a single type => static call
        //  - the value is not precise but has an upper type bound that is a subtype of the declaringClassType. 
        //  - the value is not precise and the upper type bound is a supertype 
        //    of the declaringClassType => "strange"; nevertheless, treated as a 
        //    standard virtual call with the upper type bound set to the declaring class.
        //  - the value is null => call to the constructor of NullPointerException
        //  - the value maybe null => additional call to the constructor of NullPointerException

        // TODO The following should no longer be necessary...:
        val isNull = value.isNull
        if (isNull.isYesOrUnknown) {
            implicitExceptionConstructorCall(
                classFile.thisType, method, pc,
                ObjectType.NullPointerException)
        }

        // there may be additional calls
        if (isNull.isNoOrUnknown) {
            val upperTypeBound = value.upperTypeBound
            if (upperTypeBound.consistsOfOneElement) {
                val theType = upperTypeBound.first
                if (theType.isArrayType)
                    resolvedCall(pc, ObjectType.Object, name, descriptor, true, operands)
                else if (value.isPrecise)
                    resolvedCall(pc, theType.asObjectType, name, descriptor, true, operands)
                else if ((declaringClassType ne theType) &&
                    domain.isSubtypeOf(declaringClassType, theType).isYes) {
                    // the invoke's declaring class type is "more" precise
                    println(
                        Console.YELLOW+"[warn] type information missing: "+
                            theType.toJava+" (underlying value="+receiver+")"+
                            " should be a subtype of the type of the method's declaring class: "+
                            declaringClassType.toJava+
                            " (but this cannot be deduced reliably from the project)"+Console.RESET)
                    super.unresolvedCall(pc, declaringClassType, name, descriptor, operands)
                } else {
                    super.unresolvedCall(pc, theType.asObjectType, name, descriptor, operands)
                }
            } else {
                // _Also_ supports the case where we have a "precise type", but
                // multiple types as an upper bound. This is useful in some selected
                // cases where the class is generated dynamically at runtime and 
                // hence, the currently available information is simply the best that
                // is available.

                for (utb ← upperTypeBound) {
                    if (utb.isArrayType) {
                        resolvedCall(pc, ObjectType.Object, name, descriptor, true, operands)
                    } else if ((declaringClassType ne utb) &&
                        domain.isSubtypeOf(declaringClassType, utb).isYes) {
                        // The invoke's declaring class type is "more" precise
                        println(
                            Console.YELLOW+"[warn] type information missing: "+
                                utb.toJava+"(underlying value="+receiver+")"+
                                " part of the upper type bound "+
                                upperTypeBound.map(_.toJava).mkString("(", ",", ")")+
                                " should be a subtype of the type of the method's declaring class: "+
                                declaringClassType.toJava+
                                " (but this cannot be deduced reliably from the project)"+Console.RESET)
                        doResolveCall(pc, declaringClassType, name, descriptor, operands)
                    } else {
                        val callees =
                            this.callees(pc, utb.asObjectType, name, descriptor, operands)
                        callees.filter { m ⇒
                            upperTypeBound.exists(
                                domain.isSubtypeOf(project.classFile(m).thisType, _).isYesOrUnknown
                            )
                        }
                    }
                }
            }
        }
    }
}

