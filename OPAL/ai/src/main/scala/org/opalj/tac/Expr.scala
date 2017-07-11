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

import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.Type
import org.opalj.br.FieldType
import org.opalj.br.IntegerType
import org.opalj.br.BaseType
import org.opalj.br.LongType
import org.opalj.br.FloatType
import org.opalj.br.DoubleType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.ArrayType
import org.opalj.br.MethodDescriptor
import org.opalj.br.BootstrapMethod
import org.opalj.br.MethodHandle
import org.opalj.br.PC

/**
 * Represents an expression. In general, every expression should be a simple expression, where
 * the child expressions are just `Var`s. However, when the code is going to be transformed to
 * human readable code (e.g., Java oder Scala), then it is possible to build up complex/nested
 * expressions '''after''' all transformations and static analyses have been performed.
 *
 * @tparam V
 */
trait Expr[+V <: Var[V]] extends ASTNode[V] {

    /**
     * The computational type of the underlying value.
     *
     * I.e., an approximation of the type of the underlying value. It is the best
     * type information directly available. The precision of the type information
     * depends on the number of pre-/post-processing steps that are done.
     */
    def cTpe: ComputationalType

    /**
     * `true` if the expression is ''GUARANTEED'' to have no externally observable effect if it is
     * not executed. Sideeffect free instructions can be removed if the result of the evaluation
     * of the expression is not used. For those expressions, which may result in an exception it
     * has to be guaranteed that the exception is '''NEVER''' thrown. For example, a div instruction
     * is sideeffect free if it is (statically) known that the divisor is always not equal to zero;
     * otherwise, even if the result value is not used, the expression is not (potentially) side
     * effect free.
     *
     * @note Nested expressions are not supported!
     *
     * @return `true` if the expression is ''GUARENTEED'' to have no side effect other than
     *        wasting some CPU cycles if it is not executed.
     */
    def isSideEffectFree: Boolean

    def isVar : Boolean

    private[tac] def remapIndexes(pcToIndex: Array[Int]): Unit = {}
}

/**
 * An `instance of` expression as defined by the JVM specification.
 */
case class InstanceOf[+V <: Var[V]](pc: PC, value: Expr[V], cmpTpe: ReferenceType) extends Expr[V] {

    final def astID: Int = InstanceOf.ASTID

    final def cTpe: ComputationalType = ComputationalTypeInt

    final def isSideEffectFree: Boolean = true

    final def  isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        value.remapIndexes(pcToIndex)
    }

    override def toString: String = s"InstanceOf(pc=$pc,$value,${cmpTpe.toJava})"

}
object InstanceOf { final val ASTID = -2 }

/**
 * A `checkcast` expression as defined by the JVM specification.
 */
case class Checkcast[+V <: Var[V]](pc: PC, value: Expr[V], cmpTpe: ReferenceType) extends Expr[V] {

    final def astID: Int = Checkcast.ASTID

    final def cTpe: ComputationalType = ComputationalTypeReference

    final def isSideEffectFree: Boolean = {
        // TODO Check if the type of the value is ALWAYS a subtype of cmpTpe.. then it is sideeffect free if the value expr is also sideeffect free
        false
    }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        value.remapIndexes(pcToIndex)
    }

    override def toString: String = s"Checkcast(pc=$pc,$value,${cmpTpe.toJava})"
}
object Checkcast { final val ASTID = -3 }

/**
 * A comparison of two values.
 */
case class Compare[+V <: Var[V]](
        pc:        PC,
        left:      Expr[V],
        condition: RelationalOperator,
        right:     Expr[V]
) extends Expr[V] {

    final def astID: Int = Compare.ASTID

    final def cTpe: ComputationalType = ComputationalTypeInt

    final def isSideEffectFree: Boolean = {
        assert (left.isVar && right.isVar)
        true
    }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        left.remapIndexes(pcToIndex)
        right.remapIndexes(pcToIndex)
    }

    override def toString: String = s"Compare(pc=$pc,$left,$condition,$right)"
}
object Compare { final val ASTID = -4 }

trait ValueExpr[+V <: Var[V]] extends Expr[V]

/**
 * Explicit reference to a parameter. Parameter statements '''are only used by the naive
 * representation ([[TACNaive]])''' where it is necessary to perform an initial initialization
 * of the register values.
 */
case class Param(cTpe: ComputationalType, name: String) extends ValueExpr[Nothing] {

    final def astID: Int = Param.ASTID

    final def isVar : Boolean = false

    final def isSideEffectFree: Boolean = true
}

object Param { final val ASTID = -1 }

/**
 * A constant value expression.
 */
sealed abstract class Const extends ValueExpr[Nothing] {

    final def isVar : Boolean = false

    final def isSideEffectFree: Boolean = true

}

case class MethodTypeConst(pc: PC, value: MethodDescriptor) extends Const {
    final def astID: Int = MethodTypeConst.ASTID
    final def tpe = ObjectType.MethodType
    final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"MethodTypeConst(pc=$pc,${value.toJava})"
}
object MethodTypeConst { final val ASTID = -10 }

case class MethodHandleConst(pc: PC, value: MethodHandle) extends Const {
    final def astID: Int = MethodHandleConst.ASTID
    final def tpe = ObjectType.MethodHandle
    final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"MethodHandleConst(pc=$pc,${value.toJava})"
}
object MethodHandleConst { final val ASTID = -11 }


sealed abstract class SimpleValueConst extends Const {

    def tpe: Type
}

case class IntConst(pc: PC, value: Int) extends SimpleValueConst {
    final def astID: Int = IntConst.ASTID
    final def tpe = IntegerType
    final def cTpe: ComputationalType = ComputationalTypeInt
    override def toString: String = s"IntConst(pc=$pc,$value)"
}
object IntConst { final val ASTID = -5 }

case class LongConst(pc: PC, value: Long) extends SimpleValueConst {
    final def astID: Int = LongConst.ASTID
    final def tpe = LongType
    final def cTpe: ComputationalType = ComputationalTypeLong
    override def toString: String = s"LongConst(pc=$pc,$value)"
}
object LongConst { final val ASTID = -6 }

case class FloatConst(pc: PC, value: Float) extends SimpleValueConst {
    final def astID: Int = FloatConst.ASTID
    final def tpe = FloatType
    final def cTpe: ComputationalType = ComputationalTypeFloat
    override def toString: String = s"FloatConst(pc=$pc,$value)"
}
object FloatConst { final val ASTID = -7 }

case class DoubleConst(pc: PC, value: Double) extends SimpleValueConst {
    final def astID: Int = DoubleConst.ASTID
    final def tpe = DoubleType
    final def cTpe: ComputationalType = ComputationalTypeDouble
    override def toString: String = s"DoubleConst(pc=$pc,$value)"
}
object DoubleConst { final val ASTID = -8 }

case class StringConst(pc: PC, value: String) extends SimpleValueConst {
    final def astID: Int = StringConst.ASTID
    final def tpe = ObjectType.String
    final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"StringConst(pc=$pc,$value)"
}
object StringConst { final val ASTID = -9 }

case class ClassConst(pc: PC, value: ReferenceType) extends SimpleValueConst {
    final def astID: Int = ClassConst.ASTID
    final def tpe = ObjectType.Class
    final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"ClassConst(pc=$pc,${value.toJava})"
}
object ClassConst { final val ASTID = -12 }

case class NullExpr(pc: PC) extends SimpleValueConst {
    final def astID: Int = NullExpr.ASTID
    final def tpe = ObjectType.Object // TODO Should we introduce a fake type such as "java.null"
    final def cTpe: ComputationalType = ComputationalTypeReference
    override def toString: String = s"NullExpr(pc=$pc)"
}
object NullExpr { final val ASTID = -13 }

/**
 * @param cTpe The computational type of the result of the binary expression.
 */
case class BinaryExpr[+V <: Var[V]](
        pc:   PC,
        cTpe: ComputationalType,
        op:   BinaryArithmeticOperator,
        left: Expr[V], right: Expr[V]
) extends Expr[V] {

    final def astID: Int = BinaryExpr.ASTID

    final def isSideEffectFree: Boolean = {
        // For now, we have to consider a potential "div by zero exception";
        // a better handling is only possible if we know that the value is not zero (0).
            op != BinaryArithmeticOperators.Divide ||
                (right.cTpe != ComputationalTypeInt && right.cTpe != ComputationalTypeLong)
    }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        left.remapIndexes(pcToIndex)
        right.remapIndexes(pcToIndex)
    }

    override def toString: String = s"BinaryExpr(pc=$pc,$cTpe,$left,$op,$right)"
}
object BinaryExpr { final val ASTID = -14 }

/**
 * @param cTpe The computational type of the result of the prefix expression.
 */
case class PrefixExpr[+V <: Var[V]](
        pc:      PC,
        cTpe:    ComputationalType,
        op:      UnaryArithmeticOperator,
        operand: Expr[V]
) extends Expr[V] {

    final def astID: Int = PrefixExpr.ASTID

    final def isSideEffectFree: Boolean = {assert(operand.isVar) ; true }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        operand.remapIndexes(pcToIndex)
    }

    override def toString: String = s"PrefixExpr(pc=$pc,$cTpe,$op,$operand)"
}
object PrefixExpr { final val ASTID = -15 }

case class PrimitiveTypecastExpr[+V <: Var[V]](
        pc: PC, targetTpe: BaseType,
        operand: Expr[V]
) extends Expr[V] {

    final def astID: Int = PrimitiveTypecastExpr.ASTID

    final def cTpe: ComputationalType = targetTpe.computationalType

    final def isSideEffectFree: Boolean = {assert(operand.isVar) ; true }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        operand.remapIndexes(pcToIndex)
    }
    override def toString: String = s"PrimitiveTypecastExpr(pc=$pc,$targetTpe,$operand)"
}
object PrimitiveTypecastExpr { final val ASTID = -16 }

/**
 * Allocates memory for the (non-abstract) given object. Note, that the call of the separator
 * is done later and therefore the object is not considered to be properly initialized and –
 * therefore – no further operations other than the call of a constructor are allowed.
 */
case class New(pc: PC, tpe: ObjectType) extends Expr[Nothing] {

    final def astID: Int = New.ASTID

    final def cTpe: ComputationalType = ComputationalTypeReference

    /**
     * Always returns `true` since the new instruction just allocates memory, but does NOT call the
     * constructor. Hence, except of a `java.lang.OutOfMemoryError`. which we do not
     * model any further, nothing will happen if the value is not used any further.
     */
    final def isSideEffectFree: Boolean = true

    final def isVar : Boolean = false

    override def toString: String = s"New(pc=$pc,${tpe.toJava})"
}
object New { final val ASTID = -17 }

/**
 * @param counts Encodes the number of dimensions that are initialized and the size of the
 *               respective dimension.
 * @param tpe The type of the array. The number of dimensions is always `>= count.size`.
 */
case class NewArray[+V <: Var[V]](pc: PC, counts: Seq[Expr[V]], tpe: ArrayType) extends Expr[V] {

    final def astID: Int = NewArray.ASTID

    final def cTpe: ComputationalType = ComputationalTypeReference

    final def isSideEffectFree: Boolean = {assert(counts.forall(_.isVar)) ; true }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        counts.foreach { c ⇒ c.remapIndexes(pcToIndex) }
    }

    override def toString: String = {
        s"NewArray(pc=$pc,${counts.mkString("[", ",", "]")},${tpe.toJava})"
    }
}
object NewArray { final val ASTID = -18 }

case class ArrayLoad[+V <: Var[V]](pc: PC, index: Expr[V], arrayRef: Expr[V]) extends Expr[V] {

    final def astID: Int = ArrayLoad.ASTID

    final def cTpe: ComputationalType = ComputationalTypeReference

    final def isSideEffectFree: Boolean = { assert(index.isVar && arrayRef.isVar) ; true}

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        index.remapIndexes(pcToIndex)
        arrayRef.remapIndexes(pcToIndex)
    }

    override def toString: String = s"ArrayLoad(pc=$pc,$index,$arrayRef)"
}
object ArrayLoad { final val ASTID = -19 }

case class ArrayLength[+V <: Var[V]](pc: PC, arrayRef: Expr[V]) extends Expr[V] {

    final def astID: Int = ArrayLength.ASTID

    final def cTpe: ComputationalType = ComputationalTypeInt

    final def isSideEffectFree: Boolean = {assert(arrayRef.isVar) ; true}

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        arrayRef.remapIndexes(pcToIndex)
    }

    override def toString: String = s"ArrayLength(pc=$pc,$arrayRef)"
}
object ArrayLength { final val ASTID = -20 }

abstract class FieldRead[+V <: Var[V]] extends Expr[V] {

    def declaredFieldType : FieldType

    final def cTpe: ComputationalType = declaredFieldType.computationalType

    final def isVar : Boolean = false

}

case class GetField[+V <: Var[V]](
        pc:                PC,
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType,
        objRef:            Expr[V]
) extends FieldRead[V] {

    final def astID: Int = GetField.ASTID

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        objRef.remapIndexes(pcToIndex)
    }

    final def isSideEffectFree: Boolean = { assert(objRef.isVar); true }

    override def toString: String = {
        s"GetField(pc=$pc,${declaringClass.toJava},$name,${declaredFieldType.toJava},$objRef)"
    }
}
object GetField { final val ASTID = -21 }

case class GetStatic(
        pc:                PC,
        declaringClass:    ObjectType,
        name:              String,
        declaredFieldType: FieldType
) extends FieldRead[Nothing] {

    final def astID: Int = GetStatic.ASTID

    final def isSideEffectFree: Boolean = true

    override def toString: String = {
        s"GetStatic(pc=$pc,${declaringClass.toJava},$name,${declaredFieldType.toJava})"
    }
}
object GetStatic { final val ASTID = -22 }

case class Invokedynamic[+V <: Var[V]](
        pc:              PC,
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr[V]]
) extends Expr[V] {

    final def astID: Int = Invokedynamic.ASTID

    final def cTpe: ComputationalType = descriptor.returnType.computationalType

    final def isSideEffectFree: Boolean = {
        // IMPROVE [FUTURE] Use some analysis to determine if a method call is side effect free
        false
    }

    final def isVar : Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val params = this.params.mkString("(", ",", ")")
        s"Invokedynamic(pc=$pc,$bootstrapMethod,$sig,$params)"
    }
}
object Invokedynamic { final val ASTID = -23 }

sealed abstract class FunctionCall[+V <: Var[V]] extends Expr[V] with Call[V] {

    final def cTpe: ComputationalType = descriptor.returnType.computationalType

    final def isVar : Boolean = false

}

sealed abstract class InstanceFunctionCall[+V <: Var[V]] extends FunctionCall[V] {
    def receiver: Expr[V]
}

/**
 * An instance based method call which does not require virtual method lookup. In other
 * words the target method is either directly found in the specified class or a super
 * class thereof. (Basically corresponds to an invokespecial at bytecode level.)
 *
 * @param pc The pc of the underlying, original bytecode instruction. Primarily useful to
 *           do a lookup in the line-/local-variable tables.
 * @param declaringClass The declaring class of the target method.
 * @param isInterface `true` if the declaring class defines an interface type.
 *                   (Required since Java 8.)
 * @param name The name of the target method.
 * @param descriptor The descriptor.
 * @param receiver The receiver object.
 * @param params The parameters of the method call (including the implicit `this` reference.)
 * @tparam V The type of the Var used by this representation.
 */
case class NonVirtualFunctionCall[+V <: Var[V]](
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr[V],
        params:         Seq[Expr[V]]
) extends InstanceFunctionCall[V] {

    final def astID: Int = NonVirtualFunctionCall.ASTID

    final def isSideEffectFree: Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"NonVirtualFunctionCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$receiver,$params)"
    }
}
object NonVirtualFunctionCall { final val ASTID = -24 }

case class VirtualFunctionCall[+V <: Var[V]](
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr[V],
        params:         Seq[Expr[V]]
) extends InstanceFunctionCall[V] {

    final def astID: Int = VirtualFunctionCall.ASTID

    final def isSideEffectFree: Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"VirtualFunctionCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$receiver,$params)"
    }
}
object VirtualFunctionCall { final val ASTID = -25 }

case class StaticFunctionCall[+V <: Var[V]](
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        params:         Seq[Expr[V]]
) extends FunctionCall[V] {

    final def astID: Int = StaticFunctionCall.ASTID

    final def isSideEffectFree: Boolean = false

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }

    override def toString: String = {
        val sig = descriptor.toJava(name)
        val declClass = declaringClass.toJava
        val params = this.params.mkString("(", ",", ")")
        s"StaticFunctionCall(pc=$pc,$declClass,isInterface=$isInterface,$sig,$params)"
    }
}
object StaticFunctionCall { final val ASTID = -26 }

/**
 * Represents a variable. Depending on the concrete usage, it is possible to distinguish between
 * a use and/or definition site. Typically, `V` is directly bound by the direct subtypes of Var.
 *
 * @example
 * {{{
 *     trait MyVar extends Var[MyVar]
 * }}}
 *
 * @tparam V Specifies the type of `Var` used by the three address representation. `V` is also
 *           the self type.
 */
trait Var[+V <: Var[V]] extends ValueExpr[V] { this: V ⇒

    final def astID: Int = Var.ASTID

    /**
     * A ''human readable'' name of the local variable.
     */
    def name: String

    final def isVar : Boolean = true

}

object Var {

    final val ASTID = -27

    def unapply[V <: Var[V]](variable: Var[V]): Some[String] = Some(variable.name)

}
