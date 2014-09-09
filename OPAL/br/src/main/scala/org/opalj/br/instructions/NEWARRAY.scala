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
 * Create new array handler.
 *
 * @author Michael Eichberg
 */
sealed abstract class NEWARRAY extends CreateNewArrayInstruction {

    def atype: Int = elementType.atype

    def elementType: BaseType

    final def mnemonic: String = "newarray"

    final def opcode: Opcode = NEWARRAY.opcode

    final def indexOfNextInstruction(currentPC: Int, code: Code): Int =
        indexOfNextInstruction(currentPC)

    final def indexOfNextInstruction(currentPC: PC, modifiedByWide: Boolean = false): Int =
        currentPC + 2

    final def length: Int = 2

    final override def toString: String = "NEWARRAY("+elementType.toJava+"[])"

}

private object NEWARRAY_Boolean extends NEWARRAY {
    final val elementType = BooleanType
}

private object NEWARRAY_Char extends NEWARRAY {
    final val elementType = CharType
}

private object NEWARRAY_Float extends NEWARRAY {
    final val elementType = FloatType
}

private object NEWARRAY_Double extends NEWARRAY {
    final val elementType = DoubleType
}

private object NEWARRAY_Byte extends NEWARRAY {
    final val elementType = ByteType
}

private object NEWARRAY_Short extends NEWARRAY {
    final val elementType = ShortType
}

private object NEWARRAY_Integer extends NEWARRAY {
    final val elementType = IntegerType
}

private object NEWARRAY_Long extends NEWARRAY {
    final val elementType = LongType
}

object NEWARRAY {

    final val opcode = 188

    def unapply(newarray: NEWARRAY): Option[BaseType] = Some(newarray.elementType)

    def apply(atype: Int): NEWARRAY = {
        (atype: @annotation.switch) match {
            case BooleanType.atype ⇒ NEWARRAY_Boolean
            case CharType.atype    ⇒ NEWARRAY_Char
            case FloatType.atype   ⇒ NEWARRAY_Float
            case DoubleType.atype  ⇒ NEWARRAY_Double
            case ByteType.atype    ⇒ NEWARRAY_Byte
            case ShortType.atype   ⇒ NEWARRAY_Short
            case IntegerType.atype ⇒ NEWARRAY_Integer
            case LongType.atype    ⇒ NEWARRAY_Long
        }
    }

    def getType(atype: Int): BaseType = {
        (atype: @annotation.switch) match {
            case BooleanType.atype ⇒ BooleanType
            case CharType.atype    ⇒ CharType
            case FloatType.atype   ⇒ FloatType
            case DoubleType.atype  ⇒ DoubleType
            case ByteType.atype    ⇒ ByteType
            case ShortType.atype   ⇒ ShortType
            case IntegerType.atype ⇒ IntegerType
            case LongType.atype    ⇒ LongType
        }
    }
}
