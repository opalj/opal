/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package br
package instructions

/**
 * Invoke instance method; dispatch based on class.
 *
 * @author Michael Eichberg
 */
case class INVOKEVIRTUAL(
        declaringClass:   ReferenceType, // an class or array type to be precise
        name:             String,
        methodDescriptor: MethodDescriptor
) extends VirtualMethodInvocationInstruction {

    final def isInterfaceCall: Boolean = false

    final def opcode: Opcode = INVOKEVIRTUAL.opcode

    final def mnemonic: String = "invokevirtual"

    final def jvmExceptions: List[ObjectType] = MethodInvocationInstruction.jvmExceptions

    final def length: Int = 3

    final def isInstanceMethod: Boolean = true

    // Required to avoid that Scala generates a default toString method!
    override def toString = super.toString

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object INVOKEVIRTUAL {

    final val opcode = 182

    /**
     * Factory method to create [[INVOKEVIRTUAL]] instructions.
     *
     * @param   declaringClass the method's declaring class name in JVM notation,
     *          e.g., `java/lang/Object` or `[java/lang/Object` in case of a method call on
     *          an array object. In the latter case, the called method has to be a method defined
     *          by `java/lang/Object`; e.g., `clone` or `wait`.
     * @param   isInterface has to be `true` if declaring class identifies an interface.
     *          (Determines how the target method is resolved - relevant for Java 8 onwards.)
     * @param   methodDescriptor the method descriptor in JVM notation,
     *          e.g., "()V" for a method without parameters which returns void.
     */
    def apply(
        declaringClass:   String,
        methodName:       String,
        methodDescriptor: String
    ): INVOKEVIRTUAL = {
        val declaringClassType = ReferenceType(declaringClass)
        INVOKEVIRTUAL(declaringClassType, methodName, MethodDescriptor(methodDescriptor))
    }

}
