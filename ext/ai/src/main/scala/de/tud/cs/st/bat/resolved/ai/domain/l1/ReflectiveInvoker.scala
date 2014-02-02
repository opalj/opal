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
package domain
package l1

/**
 * Offers support for performing reflective method calls. Use this trait when you need full method support 
 * for a Java type. The class which defined the method to be called needs to be in the classpath of the 
 * executing analysis environment.
 *
 * @author Frederik Buss-Joraschek
 */
trait ReflectiveInvoker extends JavaObjectConversion { this: SomeDomain ⇒

    /**
     * Performs a reflective call to the specified method. The declaring class needs to be in the classpath 
     * of the analyzing JavaVM
     *
     * Returns None when one or more parameters couldn't be transformed to a java object
     */
    def invokeReflective(pc: PC,
                         declaringClass: ReferenceType,
                         name: String,
                         descriptor: MethodDescriptor,
                         operands: List[DomainValue]): Option[MethodCallResult] = {

        val staticMethod = operands.length == descriptor.parametersCount
        val javaInstance = if (staticMethod) {
            null
        } else {
            toJavaObject(operands.last).getOrElse(return None)
        }

        try {
            val parameters = operands.take(descriptor.parametersCount)
            val javaParameters = for (value ← parameters) yield toJavaObject(value).getOrElse(return None)
            val parameterClassTypes = for (parameter ← descriptor.parameterTypes) yield parameter.toJavaClass

            val method = declaringClass.toJavaClass.getDeclaredMethod(name, parameterClassTypes: _*)
            val result = method.invoke(javaInstance, javaParameters: _*)

            if (result != null) {
                Some(ComputedValue(Some(toDomainValue(pc, result))))
            } else {
                Some(ComputedValue(None))
            }
        } catch {
            case e: ClassNotFoundException ⇒ throw DomainException("Tried to resolve a class which is not present in the classpath")
            case e: NoSuchMethodException => throw DomainException("Tried to call a method which is not declared in the class. Contains the analysis classpath the correct class?")
        }
    }
}