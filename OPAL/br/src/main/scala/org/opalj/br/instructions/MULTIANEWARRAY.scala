/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
 * Create new multidimensional array.
 *
 * @param   arrayType The type of the array to be created.
 * @param   dimensions The number of dimensions of the specified array that should be initialized.
 *
 * @author Michael Eichberg
 */
case class MULTIANEWARRAY(
        arrayType:  ArrayType,
        dimensions: Int
) extends CreateNewArrayInstruction {

    final def opcode: Opcode = MULTIANEWARRAY.opcode

    final def mnemonic: String = "multianewarray"

    final def length: Int = 4

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = dimensions

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = -dimensions + 1

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object MULTIANEWARRAY {

    final val opcode = 197

    /**
     * Factory method to create [[MULTIANEWARRAY]] instructions.
     *
     * @param   arrayType The array's type name; see [[org.opalj.br.FieldType$]] for details.
     * @param   dimensions The number of dimensions that should be initialized; the instruction will
     *          take a corresponding number of values from the stack.
     */
    def apply(arrayTypeName: String, dimensions: Int): MULTIANEWARRAY = {
        val arrayTypeCandidate = FieldType(arrayTypeName)
        require(arrayTypeCandidate.isArrayType, s"given type $arrayTypeName is not an array type")
        val arrayType = arrayTypeCandidate.asArrayType
        val arrayDimensions = arrayType.dimensions
        require(
            dimensions <= arrayDimensions,
            s"$dimensions > $arrayDimensions (the number of dimensions of the given array type)"
        )
        MULTIANEWARRAY(arrayType, dimensions)
    }

}
