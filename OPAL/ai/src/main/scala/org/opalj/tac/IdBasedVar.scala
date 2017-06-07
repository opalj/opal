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
package tac

import org.opalj.br._

/**
 * Id based variables are named based on the position of the stack/register they were
 * defined.
 * @author Roberts Kolosovs
 * @author Michael Eichberg
 */
sealed trait IdBasedVar extends Var[IdBasedVar] {

    def id: Int

    final def isSideEffectFree: Boolean = true

    /**
     * @return `true` if this variable and the given variable use the same location.
     *         Compared to `equals` this test does not consider the computational type.
     */
    final def hasSameLocation(that: IdBasedVar): Boolean = {
        that match {
            case that: IdBasedVar ⇒ this.id == that.id
            case _                ⇒ false
        }
    }

    def name: String =
        if (id == Int.MinValue) "t"
        else if (id >= 0) "op_"+id.toString
        else "r_"+(-(id + 1))

    /**
     * Creates a new variable that has the same identifier etc. but an updated
     * computational type.
     *
     * This operation is not supported for local variables!
     */
    def updated(cTpe: ComputationalType): SimpleVar = { new SimpleVar(id, cTpe) }
}

/**
 * The id determines the name of the local variable and is equivalent to "the position
 * of the value on the operand stack" or "-1-(the accessed register)".
 * If the id is Int.MinValue then the variable is an intermediate variable that
 * was artificially generated.
 */
case class SimpleVar(id: Int, cTpe: ComputationalType) extends IdBasedVar

object TempVar {

    def apply(cTpe: ComputationalType): SimpleVar = SimpleVar(Int.MinValue, cTpe)

}

object RegisterVar {

    def apply(cTpe: ComputationalType, index: UShort): SimpleVar = SimpleVar(-index - 1, cTpe)

}

object OperandVar {

    /**
     * Creates a new operand variable to store a value of the given type.
     */
    def apply(cTpe: ComputationalType, stack: List[IdBasedVar]): SimpleVar = {
        val id = stack.foldLeft(0)((c, n) ⇒ c + n.cTpe.operandSize)
        SimpleVar(id, cTpe)
    }

    /**
     * Returns the operand variable representation used for the bottom value on the stack.
     */
    def bottom(cTpe: ComputationalType): SimpleVar = {
        SimpleVar(0, cTpe)
    }

    final val IntReturnValue = OperandVar.bottom(ComputationalTypeInt)
    final val LongReturnValue = OperandVar.bottom(ComputationalTypeLong)
    final val FloatReturnValue = OperandVar.bottom(ComputationalTypeFloat)
    final val DoubleReturnValue = OperandVar.bottom(ComputationalTypeDouble)
    final val ReferenceReturnValue = OperandVar.bottom(ComputationalTypeReference)

    final val HandledException = OperandVar.bottom(ComputationalTypeReference)
}