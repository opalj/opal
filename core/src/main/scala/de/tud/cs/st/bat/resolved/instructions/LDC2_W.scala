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

import scala.language.existentials

/**
 * Push long or double from runtime constant pool.
 *
 * @author Michael Eichberg
 */
sealed abstract class LDC2_W[T <: Any] extends LoadConstantInstruction {

    def value: T

    def opcode: Int = 20

    def mnemonic: String = "ldc2_w"

    def indexOfNextInstruction(currentPC: Int, code: Code): Int = currentPC + 1 + 2

}

case class LoadLong(value: Long) extends LDC2_W[Long]

case class LoadDouble(value: Double) extends LDC2_W[Double]

object LDC2_W {

    def apply(constantValue: ConstantValue[_]): LDC2_W[_] = {
        constantValue.value match {
            case v: Long   ⇒ LoadLong(v)
            case d: Double ⇒ LoadDouble(d)
            case _         ⇒ BATException("unsupported LDC2_W constant value: "+constantValue)
        }
    }

    def unapply[T](ldc: LDC2_W[T]): Option[T] = Some(ldc.value)

}