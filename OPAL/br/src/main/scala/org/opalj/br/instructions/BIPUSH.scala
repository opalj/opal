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
 * Push byte.
 *
 * @author Michael Eichberg
 */
class BIPUSH private (
    override val value: Int)
        extends LoadConstantInstruction[Int] {

    final override def opcode: Opcode = BIPUSH.opcode

    final override def mnemonic: String = "bipush"

    override def indexOfNextInstruction(currentPC: Int, code: Code): Int = currentPC + 2

    override def equals(other: Any): Boolean = {
        other match {
            case that: BIPUSH ⇒ this eq that
            case _            ⇒ false
        }
    }

    override def hashCode = value

    override def toString = "BIPUSH("+value+")"
}

object BIPUSH {

    final val opcode = 16

    private[this] val bipushes = {
        val bipushes = new Array[BIPUSH](256)
        for (i ← -128 to 127) { bipushes(i + 128) = new BIPUSH(i) }
        bipushes
    }

    def apply(value: Int): BIPUSH = bipushes(value + 128)

    def unapply(bipush: BIPUSH): Some[Int] = Some(bipush.value)
}
