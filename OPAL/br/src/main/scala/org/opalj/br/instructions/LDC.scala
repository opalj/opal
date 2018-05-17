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
sealed abstract class LDC[@specialized(Int, Float) T] extends LoadConstantInstruction[T] {

    final def opcode: Opcode = LDC.opcode

    final def mnemonic: String = "ldc"

    final def length: Int = 2

    def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (
            this.opcode == other.opcode &&
            this.value == other.asInstanceOf[LDC[_]].value
        )
    }
}

/**
 * @note To match [[LoadInt]] and [[LoadInt_W]] instructions you can use [[LDCInt]].
 */
final case class LoadInt(value: Int) extends LDC[Int] {

    final def computationalType = ComputationalTypeInt

}

/**
 * @note To match [[LoadFloat]] and [[LoadFloat_W]] instructions you can use [[LDCFloat]].
 */
final case class LoadFloat(value: Float) extends LDC[Float] {

    final def computationalType = ComputationalTypeFloat

    override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        this.similar(other)
    }

    override def similar(other: Instruction): Boolean = {
        LDC.opcode == other.opcode && other.isInstanceOf[LoadFloat] && {
            val otherLoadFloat = other.asInstanceOf[LoadFloat]
            (this.value.isNaN && otherLoadFloat.value.isNaN) ||
                (this.value == otherLoadFloat.value)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case LoadFloat(thatValue) ⇒
                thatValue == this.value || (thatValue.isNaN && this.value.isNaN)
            case _ ⇒ false
        }
    }

    // HashCode of "value.NaN" is stable and 0
}

/**
 * @note To match [[LoadClass]] and [[LoadClass_W]] instructions you can use [[LDCClass]].
 */
final case class LoadClass(value: ReferenceType) extends LDC[ReferenceType] {
    final def computationalType = ComputationalTypeReference
}

/**
 * @note To match [[LoadMethodHandle]] and [[LoadMethodHandle_W]] instructions you
 * can use [[LDCMethodHandle]].
 */
final case class LoadMethodHandle(value: MethodHandle) extends LDC[MethodHandle] {
    final def computationalType = ComputationalTypeReference
}

/**
 * @note To match [[LoadMethodType]] and [[LoadMethodType_W]] instructions you can use
 * [[LDCMethodType]].
 */
final case class LoadMethodType(value: MethodDescriptor) extends LDC[MethodDescriptor] {
    final def computationalType = ComputationalTypeReference
}

/**
 * @note To match [[LoadString]] and [[LoadString_W]] instructions you can use [[LDCString]].
 */
final case class LoadString(value: String) extends LDC[String] {

    final def computationalType = ComputationalTypeReference

    override def toString: String = "LoadString(\""+value+"\")"

}
/**
 * Defines factory and extractor methods for LDC instructions.
 *
 * @author Michael Eichberg
 */
object LDC {

    def apply(constantValue: ConstantValue[_]): LDC[_] = {
        constantValue.value match {
            case i: Int               ⇒ LoadInt(i)
            case f: Float             ⇒ LoadFloat(f)
            case r: ReferenceType     ⇒ LoadClass(r)
            case s: String            ⇒ LoadString(s)
            case mh: MethodHandle     ⇒ LoadMethodHandle(mh)
            case md: MethodDescriptor ⇒ LoadMethodType(md)
            case _ ⇒
                throw new BytecodeProcessingFailedException(
                    "unsupported constant value: "+constantValue
                )
        }
    }

    def unapply[T](ldc: LDC[T]): Option[T] = Some(ldc.value)

    final val opcode = 18

}
