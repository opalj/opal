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

import language.existentials

/**
 * Push item from runtime constant pool.
 *
 * @author Michael Eichberg
 */
sealed abstract class LDC[@specialized(Int, Float) T]
        extends LoadConstantInstruction[T] {

    override def opcode: Opcode = LDC.opcode

    override def mnemonic: String = "ldc"

    override def indexOfNextInstruction(currentPC: Int, code: Code): Int = currentPC + 2
}

final case class LoadInt(value: Int) extends LDC[Int]

final case class LoadFloat(value: Float) extends LDC[Float]

final case class LoadClass(value: ReferenceType) extends LDC[ReferenceType]

final case class LoadMethodHandle(value: MethodHandle) extends LDC[MethodHandle]

final case class LoadMethodType(value: MethodDescriptor) extends LDC[MethodDescriptor]

final case class LoadString(value: String) extends LDC[String] {

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
                    "unsupported constant value: "+constantValue)
        }
    }

    def unapply[T](ldc: LDC[T]): Option[T] = Some(ldc.value)

    final val opcode = 18

}