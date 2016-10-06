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

import org.opalj.ai.ValueOrigin
import org.opalj.br._
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.IsPrimitiveValue

trait Expr extends ASTNode {

    /**
     * The computational type of the underlying value.
     * I.e., An approximation of the type of the underlying value. It is the best
     * type information directly available. The precision of the type information
     * depends on the number of pre-/post-processing steps that are done.
     */
    def cTpe: ComputationalType

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {}
}

trait ValueExpr extends Expr

/**
 * Parameter expressions must occur at the very beginning of the quadruples code
 * and must perform the initial initialization of the register values.
 */
case class Param(cTpe: ComputationalType, name: String) extends ValueExpr {
    final def astID = Param.ASTID
}
object Param { final val ASTID = -1 }

case class InstanceOf(pc: PC, value: Var, cmpTpe: ReferenceType) extends Expr {
    final def astID = InstanceOf.ASTID
    final def cTpe = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        value.remapIndexes(pcToIndex)
    }
}
object InstanceOf { final val ASTID = -2 }

case class Checkcast(pc: PC, value: Var, cmpTpe: ReferenceType) extends Expr {
    final def astID = Checkcast.ASTID
    final def cTpe = ComputationalTypeReference
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        value.remapIndexes(pcToIndex)
    }
}
object Checkcast { final val ASTID = -3 }

case class Compare(
        pc:        PC,
        left:      Expr,
        condition: RelationalOperator,
        right:     Expr
) extends Expr {
    final def astID = Compare.ASTID
    final def cTpe = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        left.remapIndexes(pcToIndex)
        right.remapIndexes(pcToIndex)
    }
}
object Compare { final val ASTID = -4 }

sealed trait Const extends ValueExpr

sealed trait SimpleValueConst extends Const {
    def tpe: Type
}

case class IntConst(pc: PC, value: Int) extends SimpleValueConst {
    final def astID = IntConst.ASTID
    final def tpe = IntegerType
    final def cTpe = ComputationalTypeInt
}
object IntConst { final val ASTID = -5 }

case class LongConst(pc: PC, value: Long) extends SimpleValueConst {
    final def astID = LongConst.ASTID
    final def tpe = LongType
    final def cTpe = ComputationalTypeLong
}
object LongConst { final val ASTID = -6 }

case class FloatConst(pc: PC, value: Float) extends SimpleValueConst {
    final def astID = FloatConst.ASTID
    final def tpe = FloatType
    final def cTpe = ComputationalTypeFloat
}
object FloatConst { final val ASTID = -7 }

case class DoubleConst(pc: PC, value: Double) extends SimpleValueConst {
    final def astID = DoubleConst.ASTID
    final def tpe = DoubleType
    final def cTpe = ComputationalTypeDouble
}
object DoubleConst { final val ASTID = -8 }

case class StringConst(pc: PC, value: String) extends SimpleValueConst {
    final def astID = StringConst.ASTID
    final def tpe = ObjectType.String
    final def cTpe = ComputationalTypeReference
}
object StringConst { final val ASTID = -9 }

case class MethodTypeConst(pc: PC, value: MethodDescriptor) extends Const {
    final def astID = MethodTypeConst.ASTID
    final def tpe = ObjectType.MethodType
    final def cTpe = ComputationalTypeReference
}
object MethodTypeConst { final val ASTID = -10 }

case class MethodHandleConst(pc: PC, value: MethodHandle) extends Const {
    final def astID = MethodHandleConst.ASTID
    final def tpe = ObjectType.MethodHandle
    final def cTpe = ComputationalTypeReference
}
object MethodHandleConst { final val ASTID = -11 }

case class ClassConst(pc: PC, value: ReferenceType) extends SimpleValueConst {
    final def astID = ClassConst.ASTID
    final def tpe = ObjectType.Class
    final def cTpe = ComputationalTypeReference
}
object ClassConst { final val ASTID = -12 }

case class NullExpr(pc: PC) extends SimpleValueConst {
    final def astID = NullExpr.ASTID
    final def tpe = ObjectType.Object // TODO Should we introduce a fake type such as "java.null"
    final def cTpe = ComputationalTypeReference
}
object NullExpr { final val ASTID = -13 }

/**
 * @param cTpe The computational type of the result of the binary expression.
 */
case class BinaryExpr(
        pc:   PC,
        cTpe: ComputationalType,
        op:   BinaryArithmeticOperator,
        left: Expr, right: Expr
) extends Expr {
    final def astID = BinaryExpr.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        left.remapIndexes(pcToIndex)
        right.remapIndexes(pcToIndex)
    }
}
object BinaryExpr { final val ASTID = -14 }

/**
 * @param cTpe The computational type of the result of the prefix expression.
 */
case class PrefixExpr(
        pc:      PC,
        cTpe:    ComputationalType,
        op:      UnaryArithmeticOperator,
        operand: Expr
) extends Expr {
    final def astID = PrefixExpr.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        operand.remapIndexes(pcToIndex)
    }
}
object PrefixExpr { final val ASTID = -15 }

case class PrimitiveTypecastExpr(pc: PC, targetTpe: BaseType, operand: Expr) extends Expr {
    final def astID = PrimitiveTypecastExpr.ASTID
    final def cTpe = targetTpe.computationalType
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        operand.remapIndexes(pcToIndex)
    }
}
object PrimitiveTypecastExpr { final val ASTID = -16 }

case class New(pc: PC, tpe: ObjectType) extends Expr {
    final def astID = New.ASTID
    final def cTpe = ComputationalTypeReference
}
object New { final val ASTID = -17 }

case class NewArray(pc: PC, counts: Seq[Expr], tpe: ArrayType) extends Expr {
    final def astID = NewArray.ASTID
    final def cTpe = ComputationalTypeReference
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        counts.foreach { c ⇒ c.remapIndexes(pcToIndex) }
    }
}
object NewArray { final val ASTID = -18 }

case class ArrayLoad(pc: PC, index: Var, arrayRef: Var) extends Expr {
    final def astID = ArrayLoad.ASTID
    final def cTpe = ComputationalTypeReference
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        index.remapIndexes(pcToIndex)
        arrayRef.remapIndexes(pcToIndex)
    }
}
object ArrayLoad { final val ASTID = -19 }

case class ArrayLength(pc: PC, arrayRef: Var) extends Expr {
    final def astID = ArrayLength.ASTID
    final def cTpe = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        arrayRef.remapIndexes(pcToIndex)
    }
}
object ArrayLength { final val ASTID = -20 }

case class GetField(pc: PC, declaringClass: ObjectType, name: String, objRef: Expr) extends Expr {
    final def astID = GetField.ASTID
    final def cTpe = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        objRef.remapIndexes(pcToIndex)
    }
}
object GetField { final val ASTID = -21 }

case class GetStatic(pc: PC, declaringClass: ObjectType, name: String) extends Expr {
    final def astID = GetStatic.ASTID
    final def cTpe = ComputationalTypeInt
}
object GetStatic { final val ASTID = -22 }

case class Invokedynamic(
        pc:              PC,
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr]
) extends Expr {
    final def astID = Invokedynamic.ASTID
    final def cTpe = descriptor.returnType.computationalType
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object Invokedynamic { final val ASTID = -23 }

sealed trait FunctionCall extends Call with Expr {
    final def cTpe = descriptor.returnType.computationalType
}

sealed trait InstanceFunctionCall extends FunctionCall {
    def receiver: Expr
}

case class NonVirtualFunctionCall(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr,
        params:         Seq[Expr]
) extends InstanceFunctionCall {
    final def astID = NonVirtualFunctionCall.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object NonVirtualFunctionCall { final val ASTID = -24 }

case class VirtualFunctionCall(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr,
        params:         Seq[Expr]
) extends InstanceFunctionCall {
    final def astID = VirtualFunctionCall.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object VirtualFunctionCall { final val ASTID = -25 }

case class StaticFunctionCall(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        params:         Seq[Expr]
) extends FunctionCall {
    final def astID = StaticFunctionCall.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object StaticFunctionCall { final val ASTID = -26 }

trait Var extends ValueExpr {

    final def astID = Var.ASTID

    /**
     * Calling this method is only supported if the underlying representation is in
     * SSA like form. I.e., each local variable has a single static definition site!
     *
     * @note    Calling this method is generally safe if the quadruples representation was
     *           created using the result of an abstract interpretation.
     */
    def asSSAVar: SSAVar = {
        throw new ClassCastException(this.getClass().getName+" cannot be cast to SSAVar")
    }

    /**
     * A human readable name of the local variable.
     */
    def name: String

    /**
     * @return `true` if this variable and the given variable use the same location.
     * 			Compared to `equals` this test does not consider the computational type.
     */
    def hasSameLocation(that: Var): Boolean

    /**
     * Creates a new variable that has the same identifier etc. but an updated
     * computational type.
     *
     * This operation is not supported for local variables!
     */
    def updated(cTpe: ComputationalType): Var
}

object Var {
    final val ASTID = -27
    def unapply(variable: Var): Some[String] = Some(variable.name)
}

/**
 * Identifies a variable which has a single static definition/initialization site.
 *
 * @param origin Identifies the single index(pc) of the instruction which initialized
 *          the variable. I.e., per method there must be at most one SSA variable which
 *          has the given origin.
 *          Initially, the pc of the underlying bytecode instruction is used.
 */
abstract class SSAVar(private[tac] var origin: ValueOrigin) extends Var {

    def tpe: Type

    def cTpe: ComputationalType = tpe.computationalType

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        origin = pcToIndex(origin)
    }

    /**
     * This variable's definition site. Only defined after the transformation is complete!
     */
    final def defSite: ValueOrigin = origin

    final override def asSSAVar: this.type = this

    def name: String = "l"+origin

    def hasSameLocation(other: Var): Boolean = {
        val thisOrigin = this.origin
        other match { case that: SSAVar ⇒ thisOrigin == that.origin; case _ ⇒ false }
    }

    def updated(cTpe: ComputationalType): Var = throw new UnsupportedOperationException()

}
class SSAPrimVar(
    private[tac]origin:  ValueOrigin,
    val tpe:             BaseType,
    val primValue:       Option[IsPrimitiveValue]
) extends SSAVar(origin)
object SSAPrimVar {
    def apply(
        origin:    ValueOrigin,
        tpe:       BaseType,
        primValue: Option[IsPrimitiveValue] = None
    ): SSAPrimVar = {
        new SSAPrimVar(origin, tpe, primValue)
    }
}

class SSARefVar(
    private[tac]origin:  ValueOrigin,
    val tpe:             ReferenceType,
    val refValue:        Option[IsAReferenceValue] = None
) extends SSAVar(origin)
object SSARefVar {
    def apply(
        origin:   ValueOrigin,
        tpe:      ReferenceType,
        refValue: Option[IsAReferenceValue] = None
    ): SSARefVar = {
        new SSARefVar(origin, tpe, refValue)
    }
}

/**
 * Id based variables are named based on the position of the stack/register they were
 * defined.
 */
sealed trait IdBasedVar extends Var {

    def id: Int

    final def hasSameLocation(that: Var): Boolean = {
        that match {
            case that: IdBasedVar ⇒ this.id == that.id
            case _                ⇒ false
        }
    }

    def name =
        if (id == Int.MinValue) "t"
        else if (id >= 0) "op_"+id.toString
        else "r_"+(-(id + 1))

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
    def apply(cTpe: ComputationalType, stack: Stack): SimpleVar = {
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
