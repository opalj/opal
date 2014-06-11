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
package br
package instructions

/**
 * Represents an "incomplete" invoke dynamic instruction. Here, incomplete refers
 * to the fact that not all information is yet available because it is not
 * yet loaded. In case of `invokedynamic` instructions it is necessary
 * to read a class file's attributes which are read in at the very end. This requires
 * to resolve INVOKEDYNAMIC instructions in a two step process.
 *
 * @author Michael Eichberg
 */
case object INCOMPLETE_INVOKEDYNAMIC extends InvocationInstruction {

    private def error: Nothing =
        throw new BytecodeProcessingFailedException(
            "this invokedynamic instruction was not resolved")

    final def bootstrapMethod: BootstrapMethod = error

    final def name: String = error

    final def methodDescriptor: MethodDescriptor = error

    final val opcode = INVOKEDYNAMIC.opcode

    final def mnemonic: String = "invokedynamic"

    final def length: Int = 5

    final def runtimeExceptions: List[ObjectType] = INVOKEDYNAMIC.runtimeExceptions

}

/**
 * Represents an `invokedynamic` instruction.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
trait INVOKEDYNAMIC extends InvocationInstruction {

    /*abstract*/ def bootstrapMethod: BootstrapMethod

    final def opcode: Opcode = INVOKEDYNAMIC.opcode

    final def mnemonic: String = "invokedynamic"

    final def length: Int = 5

    final def runtimeExceptions: List[ObjectType] = INVOKEDYNAMIC.runtimeExceptions

    override def toString: String =
        "INVOKEDYNAMIC\n"+
            bootstrapMethod.toString+"\n"+
            "Target("+name+" "+methodDescriptor.toUMLNotation+")"
}

/**
 * Represents an `invokedynamic` instruction where we have no further, immediately usable
 * information regarding the target.
 *
 * @param bootstrapMethod This is the bootstrap method that needs to be executed in order
 * 		to resolve the instruction's target.
 * @param name This is the name of the method that this invokedynamic instruction intends
 * 		to invoke.
 * @param methodDescriptor This is the descriptor belonging to the instruction's intended
 * 		invocation target.
 *
 * @author Arne Lottmann
 */
case class UNRESOLVED_INVOKEDYNAMIC(
    bootstrapMethod: BootstrapMethod,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends INVOKEDYNAMIC

/**
 * Helper methods needed for the resolution of `invokedynamic` instructions.
 */
object INVOKEDYNAMIC {

    final val runtimeExceptions = List(ObjectType.BootstrapMethodError)

    final val opcode = 186

    /**
     * General extractor for objects of type `INVOKEDYNAMIC`.
     */
    def unapply(instruction: INVOKEDYNAMIC): Option[(BootstrapMethod, String, MethodDescriptor)] =
        Some((instruction.bootstrapMethod, instruction.name, instruction.methodDescriptor))
}

