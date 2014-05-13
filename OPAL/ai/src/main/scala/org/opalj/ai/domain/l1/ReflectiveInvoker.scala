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
package domain
package l1

import br._

/**
 * Support the invocation of methods (using Java reflection) of Java objects that
 * represent concrete domain values.
 *
 * This greatly facilitates the implementation of methods that need to simulate
 * the logic of specific object.
 *
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait ReflectiveInvoker extends JavaObjectConversion { this: Domain ⇒

    def warnOnFailedReflectiveCalls: Boolean = true

    /**
     * Performs a reflective call to the specified method. The declaring class needs
     * to be in the classpath of the analyzing JavaVM.
     *
     * @see [[JavaObjectConversion]] for details.
     *
     * @return `None` when the reflective invocation has failed.
     */
    def invokeReflective(
        pc: PC,
        declaringClass: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        operands: List[DomainValue]): Option[MethodCallResult] = {

        val (method, jReceiver, jOperands) =
            try {
                val declaredParametersCount = descriptor.parametersCount
                var operandCount = 0
                var jReceiver: Object = null
                var jOperands: List[Object] = Nil
                // Recall: the last method parameter is the top-most stack value ... 
                // the receiver (if existing) is the last operand.
                operands foreach { op ⇒
                    operandCount += 1
                    val jObject =
                        toJavaObject(op) match {
                            case Some(jObject) ⇒ jObject
                            case _             ⇒ return None /* <------- EARLY RETURN FROM METHOD */
                        }
                    if (operandCount > declaredParametersCount) {
                        // this is also the last operand
                        jReceiver = jObject
                    } else {
                        jOperands = jObject :: jOperands
                    }
                }
                val jParameterClassTypes = descriptor.parameterTypes map (_.toJavaClass)
                // TODO [improvement] Look for signature compatible methods. E.g., If the current type is String and the method is equals, we should not only look for the method equals(String), but also equals(Object)
                val method = declaringClass.toJavaClass.getDeclaredMethod(
                    name, jParameterClassTypes: _*
                )
                (method, jReceiver, jOperands)
            } catch {
                case e: ClassNotFoundException ⇒
                    if (warnOnFailedReflectiveCalls)
                        Console.println(
                            Console.RED+
                                "[warn] Delegating calls to the concrete Java object is not possible: \""+
                                e.getMessage()+
                                "\" class is not found on the JVM's classpath."+
                                Console.RESET
                        )
                    return None /* <------- EARLY RETURN FROM METHOD */
                case e: NoSuchMethodException ⇒
                    if (warnOnFailedReflectiveCalls)
                        Console.println(
                            Console.RED+
                                "[warn] The method \""+descriptor.toJava(name)+
                                "\" is not defined by the class on the JVM's class path: "+
                                declaringClass.toJava+"."+
                                Console.RESET
                        )
                    return None /* <------- EARLY RETURN FROM METHOD */
            }

        try {
            val result = method.invoke(jReceiver, jOperands: _*)
            (descriptor.returnType.id: @scala.annotation.switch) match {
                case VoidType.id ⇒
                    Some(ComputationWithSideEffectOnly)
                case BooleanType.id ⇒
                    Some(ComputedValue(Some(BooleanValue(
                        pc,
                        result.asInstanceOf[java.lang.Boolean].booleanValue()
                    ))))
                case ByteType.id ⇒
                    Some(ComputedValue(Some(ByteValue(
                        pc,
                        result.asInstanceOf[java.lang.Byte].byteValue()
                    ))))
                case ShortType.id ⇒
                    Some(ComputedValue(Some(ShortValue(
                        pc,
                        result.asInstanceOf[java.lang.Short].shortValue()
                    ))))
                case CharType.id ⇒
                    Some(ComputedValue(Some(CharValue(
                        pc,
                        result.asInstanceOf[java.lang.Character].charValue()
                    ))))
                case IntegerType.id ⇒
                    Some(ComputedValue(Some(IntegerValue(
                        pc,
                        result.asInstanceOf[java.lang.Integer].intValue()
                    ))))
                case LongType.id ⇒
                    Some(ComputedValue(Some(LongValue(
                        pc,
                        result.asInstanceOf[java.lang.Long].longValue()
                    ))))
                case FloatType.id ⇒
                    Some(ComputedValue(Some(FloatValue(
                        pc,
                        result.asInstanceOf[java.lang.Float].floatValue()
                    ))))
                case DoubleType.id ⇒
                    Some(ComputedValue(Some(DoubleValue(
                        pc,
                        result.asInstanceOf[java.lang.Double].doubleValue()
                    ))))
                case _ ⇒
                    Some(ComputedValue(Some(toDomainValue(pc, result))))
            }
        } catch {
            // The exception happens as part of the execution of the underlying method;
            // hence, we want to capture it and use it in the following!
            case npe: NullPointerException ⇒
                Some(justThrows(NullPointerException(pc)))
            case ite: java.lang.reflect.InvocationTargetException ⇒
                Some(justThrows(toDomainValue(pc, ite.getCause())))
        }
    }
}