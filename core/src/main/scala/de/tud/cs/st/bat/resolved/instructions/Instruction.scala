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

/**
 * Common superclass of all instructions.
 *
 * @author Michael Eichberg
 */
trait Instruction {

    /**
     *  The opcode of the instruction as defined by the JVM specification.
     */
    def opcode: Int

    /**
     *  The mnemonic of the instruction as defined by the JVM specification.
     */
    def mnemonic: String

    /**
     * The exceptions that may be thrown by the JVM at runtime if the execution of this instruction fails.
     * I.e., these are neither exceptions that are explicitly created and then thrown by user code nor
     * errors that my arise due to an invalid code base.
     */
    def runtimeExceptions: List[ObjectType]

    /**
     * The index of the next instruction in the code array.
     */
    def indexOfNextInstruction(currentPC: Int, code: Code): Int

    def toString(currentPC: Int): String = toString()

}
/**
 * Functionality common to instructions.
 *
 * @author Michael Eichberg
 */
object Instruction {

    def unapply(instruction: Instruction): Option[(Int, String, List[ObjectType])] = {
        Some((instruction.opcode, instruction.mnemonic, instruction.runtimeExceptions))
    }
}