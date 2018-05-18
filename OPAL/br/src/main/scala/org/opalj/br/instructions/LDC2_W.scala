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

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Push long or double from runtime constant pool.
 *
 * @author Michael Eichberg
 */
sealed abstract class LDC2_W[@specialized(Long, Double) T <: Any]
    extends LoadConstantInstruction[T] {

    final def opcode: Opcode = LDC2_W.opcode

    final def mnemonic: String = "ldc2_w"

    final def length: Int = 3

}

final case class LoadLong(value: Long) extends LDC2_W[Long] {

    final def computationalType = ComputationalTypeLong

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (
            LDC2_W.opcode == other.opcode && other.isInstanceOf[LoadLong] &&
            this.value == other.asInstanceOf[LoadLong].value
        )
    }

}

final case class LoadDouble(value: Double) extends LDC2_W[Double] {

    final def computationalType = ComputationalTypeDouble

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        this.similar(other)
    }

    override def similar(other: Instruction): Boolean = {
        (this eq other) || (
            LDC2_W.opcode == other.opcode && other.isInstanceOf[LoadDouble] && {
                val otherLoadDouble = other.asInstanceOf[LoadDouble]
                (this.value.isNaN && otherLoadDouble.value.isNaN) ||
                    (this.value == otherLoadDouble.value)
            }
        )
    }

    override def equals(other: Any): Boolean = {
        other match {
            case LoadDouble(thatValue) ⇒
                thatValue == this.value || (thatValue.isNaN && this.value.isNaN)
            case _ ⇒ false
        }
    }

    // HashCode of "value.NaN" is stable and 0

}

/**
 * Defines factory and extractor methods for LDC2_W instructions.
 *
 * @author Michael Eichberg
 */
object LDC2_W {

    final val opcode = 20

    def apply(constantValue: ConstantValue[_]): LDC2_W[_] = {
        constantValue.value match {
            case v: Long   ⇒ LoadLong(v)
            case d: Double ⇒ LoadDouble(d)
            case _ ⇒
                throw new BytecodeProcessingFailedException(
                    "unsupported LDC2_W constant value: "+constantValue
                )
        }
    }

    def unapply[T](ldc: LDC2_W[T]): Option[T] = Some(ldc.value)

}
