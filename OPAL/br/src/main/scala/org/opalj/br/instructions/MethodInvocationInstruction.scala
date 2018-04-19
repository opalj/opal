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
 * An instruction that invokes another method (does not consider invokedynamic
 * instructions).
 *
 * @author Michael Eichberg
 */
abstract class MethodInvocationInstruction extends InvocationInstruction {

    final override def isMethodInvocationInstruction: Boolean = true
    final override def asMethodInvocationInstruction: MethodInvocationInstruction = this

    /* abstract */ def declaringClass: ReferenceType

    def isInterfaceCall: Boolean

    /**
     * Returns the number of registers required to store the method's arguments
     * including (if required) the self reference "this".
     */
    def count: Int = {
        // c.f. JVM 8 Spec. Section 6.5.
        (if (isInstanceMethod) 1 else 0) + methodDescriptor.requiredRegisters
    }

    /**
     * Returns `true` if the called method is an instance method and virtual method
     * call resolution has to take place. I.e., if the underlying instruction is an
     * invokevirtual or an invokeinterface instruction.
     */
    /* abstract */ def isVirtualMethodCall: Boolean

    def asVirtualMethod: VirtualMethod = VirtualMethod(declaringClass, name, methodDescriptor)

    override def toString: String = {
        s"${this.getClass.getSimpleName}(${methodDescriptor.toJava(declaringClass.toJava, name)})"
    }

}

/**
 * Defines commonly used constants and an extractor method to match [[MethodInvocationInstruction]]
 * instructions.
 */
object MethodInvocationInstruction {

    def unapply(
        instruction: MethodInvocationInstruction
    ): Option[(ReferenceType, Boolean, String, MethodDescriptor)] = {
        Some((
            instruction.declaringClass,
            instruction.isInterfaceCall,
            instruction.name,
            instruction.methodDescriptor
        ))
    }

    val jvmExceptions = List(ObjectType.NullPointerException)

}
