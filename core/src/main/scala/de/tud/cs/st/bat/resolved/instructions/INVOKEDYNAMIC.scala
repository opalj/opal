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
package instructions

/**
 * Represents an "unresolved" invoke dynamic instruction. Here, unresolved refers
 * to the fact that no all information are yet available because they are not
 * yet loaded. To successfully resolve invokedynamic instructions it is necessary
 * to read a class file's attributes which are read in at the very end. This requires
 * that invoke dynamic instructions can only be resolved in a second step.
 *
 * @author Michael Eichberg
 */
case object UNRESOLVED_INVOKEDYNAMIC extends InvocationInstruction {

    private def error: Nothing =
        throw new BATException("this invokedynamic instruction was not resolved")

    def bootstrapMethod: BootstrapMethod = error

    override def name: String = error

    override def methodDescriptor: MethodDescriptor = error

    override def opcode: Int = 186

    override def mnemonic: String = "invokedynamic"

    override def indexOfNextInstruction(currentPC: Int, code: Code): Int = currentPC + 5

    override def runtimeExceptions: List[ObjectType] = INVOKEDYNAMIC.runtimeExceptions

}

/**
 * Invoke dynamic.
 *
 * @author Michael Eichberg
 */
case class INVOKEDYNAMIC(
    val bootstrapMethod: BootstrapMethod,
    override val name: String,
    override val methodDescriptor: MethodDescriptor)
        extends InvocationInstruction {

    override def opcode: Int = 186

    override def mnemonic: String = "invokedynamic"

    override def indexOfNextInstruction(currentPC: Int, code: Code): Int = currentPC + 5

    override def runtimeExceptions: List[ObjectType] = INVOKEDYNAMIC.runtimeExceptions

    override def toString: String =
        "INVOKEDYNAMIC\n"+
            bootstrapMethod.toString+"\n"+
            "Target("+name+" "+methodDescriptor.toUMLNotation+")"

}

object INVOKEDYNAMIC {

    val runtimeExceptions = List(ObjectType.BootstrapMethodError)

}


