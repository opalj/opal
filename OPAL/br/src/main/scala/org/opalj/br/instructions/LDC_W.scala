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
 * Push item from runtime constant pool.
 *
 * @author Michael Eichberg
 */
sealed abstract class LDC_W[T] extends LoadConstantInstruction[T] {

    final def opcode: Opcode = LDC_W.opcode

    final def mnemonic: String = "ldc_w"

    final def length: Int = 3

    def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (
            LDC_W.opcode == other.opcode &&
            this.value == other.asInstanceOf[LDC_W[_]].value
        )
    }

}

final case class LoadInt_W(value: Int) extends LDC_W[Int] {
    final def computationalType = ComputationalTypeInt
}

final case class LoadFloat_W(value: Float) extends LDC_W[Float] {

    final def computationalType = ComputationalTypeFloat

    override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        this.similar(other)
    }

    override def similar(other: Instruction): Boolean = {
        (this eq other) || (
            LDC_W.opcode == other.opcode && other.isInstanceOf[LoadFloat_W] && {
                val otherLoadFloat = other.asInstanceOf[LoadFloat_W]
                (this.value.isNaN && otherLoadFloat.value.isNaN) ||
                    (this.value == otherLoadFloat.value)
            }
        )
    }

    override def equals(other: Any): Boolean = {
        other match {
            case LoadFloat_W(thatValue) ⇒
                thatValue == this.value || (thatValue.isNaN && this.value.isNaN)
            case _ ⇒ false
        }
    }
}

final case class LoadClass_W(value: ReferenceType) extends LDC_W[ReferenceType] {
    final def computationalType : ComputationalTypeReference.type = ComputationalTypeReference
}

final case class LoadMethodHandle_W(value: MethodHandle) extends LDC_W[MethodHandle] {
    final def computationalType : ComputationalTypeReference.type  = ComputationalTypeReference
}

final case class LoadMethodType_W(value: MethodDescriptor) extends LDC_W[MethodDescriptor] {
    final def computationalType : ComputationalTypeReference.type  = ComputationalTypeReference
}

/**
 * @note To match [[LoadString]] and [[LoadString_W]] instructions you can use [[LDCString]].
 */
final case class LoadString_W(value: String) extends LDC_W[String] {
    final def computationalType = ComputationalTypeReference
}

/**
 * Defines factory and extractor methods for LDC_W instructions.
 *
 * @author Michael Eichberg
 */
object LDC_W {

    final val opcode = 19

    def apply(constantValue: ConstantValue[_]): LDC_W[_] = {
        constantValue.value match {
            case i: Int               ⇒ LoadInt_W(i)
            case f: Float             ⇒ LoadFloat_W(f)
            case r: ReferenceType     ⇒ LoadClass_W(r)
            case s: String            ⇒ LoadString_W(s)
            case mh: MethodHandle     ⇒ LoadMethodHandle_W(mh)
            case md: MethodDescriptor ⇒ LoadMethodType_W(md)
            case _ ⇒
                throw new BytecodeProcessingFailedException("unsupported value: "+constantValue)
        }
    }

    def unapply[T](ldc: LDC_W[T]): Option[T] = Some(ldc.value)
}
