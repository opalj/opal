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
 * An arithmetic instruction as defined by the JVM specification.
 *
 * @author Michael Eichberg
 */
trait ArithmeticInstruction extends Instruction {

    /**
     * A string representation of the operator as
     * used by the Java programming language (if possible).
     * (In case of the comparison operators for long, float and double values the
     * strings `cmp(g|l)?` are used.
     */
    def operator: String

    /**
     * The computational type of the '''primary value(s)''' processed by the instruction.
     *
     * @note In case of the shift instructions for int and long values the second value
     *      is always an int value but in both cases not all bits are taken into account.
     */
    def computationalType: ComputationalType

    /**
     * Returns `true` if this instruction is a shift (`<<`, `>>`, `>>>`) instruction.
     * [[ShiftInstruction]]s are special since the computational type of the shift
     * value must not be the same as the computational type of the shifted value and
     * not all bits are taken into account.
     */
    def isShiftInstruction: Boolean
}

/**
 * Defines values and methods common to arithmetic instructions.
 *
 * @author Michael Eichberg
 */
object ArithmeticInstruction {

    final val runtimeExceptions = List(ObjectType.ArithmeticException)

}

