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
package instructions

/**
 * An instruction that "invokes" something. This can be, e.g., the invocation of a method
 * or the dynamic invocation (using `invokedynamic`) of a field read.
 *
 * @author Michael Eichberg
 */
abstract class InvocationInstruction extends Instruction {

    def name: String

    def methodDescriptor: MethodDescriptor

    final override def nextInstructions(currentPC: PC, code: Code): PCs = {
        // Here, we basically have no idea which exceptions may be thrown hence,
        // we make the safe assumption that any handler is a potential successor!
        val excpetionHandlerPCs = Instruction.allExceptionHandlers(currentPC, code)
        excpetionHandlerPCs + indexOfNextInstruction(currentPC, code)
    }
}

/**
 * An instruction that invokes another method (does not consider invokedynamic
 * instructions.)
 *
 * @author Michael Eichberg
 */
abstract class MethodInvocationInstruction extends InvocationInstruction {

    def declaringClass: ReferenceType

    def isVirtualMethodCall: Boolean

    override def toString: String =
        this.getClass().getSimpleName()+"\n"+
            declaringClass.toJava+"\n"+name+" "+methodDescriptor.toUMLNotation

}

abstract class VirtualMethodInvocationInstruction extends MethodInvocationInstruction {
    def isVirtualMethodCall: Boolean = true
}

/**
 * Invocation of a method where the target method is statically resovled.
 *
 * @author Michael Eichberg
 */
abstract class StaticMethodInvocationInstruction extends MethodInvocationInstruction {
    def isVirtualMethodCall: Boolean = false
}

object MethodInvocationInstruction {

    def unapply(instruction: MethodInvocationInstruction): Option[(ReferenceType, String, MethodDescriptor)] = {
        Some(instruction.declaringClass, instruction.name, instruction.methodDescriptor)
    }

    val runtimeExceptions = List(ObjectType.NullPointerException)

}