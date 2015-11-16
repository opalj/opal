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
package tac

import scala.collection.mutable.BitSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import org.opalj.collection.mutable.Locals
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.ai.Domain
import org.opalj.br._

trait Expr {

    /**
     * An approximation of the type of the underlying value. It is the best
     * type information directly available. The precision of the type information
     * depends on the number of post-processing steps that are done.
     */
    def cTpe: ComputationalType
}

/**
 * Parameter expressions must occur at the very beginning of the quadruples code
 * and must perform the initial initialization of the register values.
 */
case class Param(cTpe: ComputationalType, name: String) extends Expr

case class InstanceOf(pc: PC, value: Var, cmpTpe: ReferenceType) extends Expr {
    final def cTpe = ComputationalTypeInt
}

case class Checkcast(pc: PC, value: Var, cmpTpe: ReferenceType) extends Expr {
    final def cTpe = ComputationalTypeReference
}

case class Compare(
    pc:        PC,
    left:      Expr,
    condition: RelationalOperator,
    right:     Expr
)
        extends Expr {

    final def cTpe: ComputationalType = ComputationalTypeInt

}

sealed trait Const extends Expr

case class IntConst(pc: PC, value: Int) extends Expr {
    final def tpe = IntegerType
    final def cTpe = ComputationalTypeInt
}

case class LongConst(pc: PC, value: Long) extends Expr {
    final def tpe = LongType
    final def cTpe = ComputationalTypeLong
}

case class FloatConst(pc: PC, value: Float) extends Expr {
    final def tpe = FloatType
    final def cTpe = ComputationalTypeFloat
}

case class DoubleConst(pc: PC, value: Double) extends Expr {
    final def tpe = DoubleType
    final def cTpe = ComputationalTypeDouble
}

case class StringConst(pc: PC, value: String) extends Expr {
    final def tpe = ObjectType.String
    final def cTpe = ComputationalTypeReference
}

case class MethodTypeConst(pc: PC, value: MethodDescriptor) extends Expr {
    final def tpe = ObjectType.MethodType
    final def cTpe = ComputationalTypeReference
}

case class MethodHandleConst(pc: PC, value: MethodHandle) extends Expr {
    final def tpe = ObjectType.MethodHandle
    final def cTpe = ComputationalTypeReference
}

case class ClassConst(pc: PC, value: ReferenceType) extends Expr {
    final def tpe = ObjectType.Class
    final def cTpe = ComputationalTypeReference
}

case class NullExpr(pc: PC) extends Expr {
    final def cTpe = ComputationalTypeReference
}

/**
 * @param cTpe The computational type of the result of the binary expression.
 */
case class BinaryExpr(
    pc:   PC,
    cTpe: ComputationalType,
    op:   BinaryArithmeticOperator,
    left: Expr, right: Expr
) extends Expr

case class PrefixExpr(
    pc:      PC,
    cTpe:    ComputationalType,
    op:      UnaryArithmeticOperator,
    operand: Expr
) extends Expr

case class PrimitiveTypecastExpr(
        pc:        PC,
        targetTpe: BaseType,
        operand:   Expr
) extends Expr {
    final def cTpe = targetTpe.computationalType
}

case class New(
        pc:  PC,
        tpe: ObjectType
) extends Expr {
    final def cTpe = ComputationalTypeReference
}

case class NewArray(
        pc:     PC,
        counts: List[Expr],
        tpe:    ArrayType
) extends Expr {
    final def cTpe = ComputationalTypeReference
}

case class ArrayLoad(pc: PC, index: Var, arrayRef: Var) extends Expr {
    final def cTpe = ComputationalTypeReference
}

case class ArrayLength(pc: PC, arrayRef: Var) extends Expr {
    final def cTpe = ComputationalTypeInt
}

case class GetField(
    pc:             PC,
    declaringClass: ObjectType, name: String, objRef: Expr
)
        extends Expr {
    final def cTpe = ComputationalTypeInt
}

case class GetStatic(
    pc:             PC,
    declaringClass: ObjectType, name: String
)
        extends Expr {
    final def cTpe = ComputationalTypeInt
}

trait Var extends Expr {

    /**
     * A human readable name of the local variable.
     */
    def name: String

    /**
     * Creates a new variable that has the same identifier etc. but an updated
     * type.
     */
    def updated(cTpe: ComputationalType): Var
}

object Var {

    def unapply(variable: Var): Some[String] = Some(variable.name)

}

trait IdBasedVar extends Var {

    def id: Int

    def name =
        if (id == Int.MinValue) "t"
        else if (id >= 0) "op_"+id.toString
        else "r_"+(-(id + 1))

    def updated(cTpe: ComputationalType): SimpleVar = {
        new SimpleVar(id, cTpe)
    }
}

/**
 * The id determines the name of the local variable and is equivalent to "the position
 * of the value on the operand stack" or "-1-(the accessed register)".
 * If the id is Int.MinValue then the variable is an intermediate variable that
 * was artificially generated.
 */
case class SimpleVar(id: Int, cTpe: ComputationalType) extends IdBasedVar {

}
case class DomainValueBasedVar(id: Int, properties: Domain#DomainValue) extends IdBasedVar {

    final override def cTpe = properties.computationalType
}
object TempVar {

    def apply(cTpe: ComputationalType): SimpleVar = SimpleVar(Int.MinValue, cTpe)

}
object RegisterVar {

    def apply(cTpe: ComputationalType, index: UShort): SimpleVar = {
        SimpleVar(-index - 1, cTpe)
    }

}

object OperandVar {

    /**
     * Creates a new operand variable to store a value of the given type.
     */
    def apply(cTpe: ComputationalType, stack: Stack): SimpleVar = {
        val id = stack.foldLeft(0)((c, n) ⇒ c + n.cTpe.category)
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

