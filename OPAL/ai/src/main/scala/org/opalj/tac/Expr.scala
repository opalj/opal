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

import org.opalj.collection.immutable.IntSet

import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.Type
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
import org.opalj.ai.ValueOrigin

trait Expr extends ASTNode {

    /**
     * The computational type of the underlying value.
     * I.e., an approximation of the type of the underlying value. It is the best
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
    final def astID: Int = Param.ASTID
}
object Param { final val ASTID = -1 }

case class InstanceOf(pc: PC, value: Var, cmpTpe: ReferenceType) extends Expr {
    final def astID: Int = InstanceOf.ASTID
    final def cTpe: ComputationalType = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        value.remapIndexes(pcToIndex)
    }
}
object InstanceOf { final val ASTID = -2 }

case class Checkcast(pc: PC, value: Var, cmpTpe: ReferenceType) extends Expr {
    final def astID: Int = Checkcast.ASTID
    final def cTpe: ComputationalType = ComputationalTypeReference
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
    final def astID: Int = Compare.ASTID
    final def cTpe: ComputationalType = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        left.remapIndexes(pcToIndex)
        right.remapIndexes(pcToIndex)
    }
}
object Compare { final val ASTID = -4 }

sealed abstract class Const extends ValueExpr

sealed abstract class SimpleValueConst extends Const {
    def tpe: Type
}

case class IntConst(pc: PC, value: Int) extends SimpleValueConst {
    final def astID: Int = IntConst.ASTID
    final def tpe = IntegerType
    final def cTpe: ComputationalType = ComputationalTypeInt
}
object IntConst { final val ASTID = -5 }

case class LongConst(pc: PC, value: Long) extends SimpleValueConst {
    final def astID: Int = LongConst.ASTID
    final def tpe = LongType
    final def cTpe: ComputationalType = ComputationalTypeLong
}
object LongConst { final val ASTID = -6 }

case class FloatConst(pc: PC, value: Float) extends SimpleValueConst {
    final def astID: Int = FloatConst.ASTID
    final def tpe = FloatType
    final def cTpe: ComputationalType = ComputationalTypeFloat
}
object FloatConst { final val ASTID = -7 }

case class DoubleConst(pc: PC, value: Double) extends SimpleValueConst {
    final def astID: Int = DoubleConst.ASTID
    final def tpe = DoubleType
    final def cTpe: ComputationalType = ComputationalTypeDouble
}
object DoubleConst { final val ASTID = -8 }

case class StringConst(pc: PC, value: String) extends SimpleValueConst {
    final def astID: Int = StringConst.ASTID
    final def tpe = ObjectType.String
    final def cTpe: ComputationalType = ComputationalTypeReference
}
object StringConst { final val ASTID = -9 }

case class MethodTypeConst(pc: PC, value: MethodDescriptor) extends Const {
    final def astID: Int = MethodTypeConst.ASTID
    final def tpe = ObjectType.MethodType
    final def cTpe: ComputationalType = ComputationalTypeReference
}
object MethodTypeConst { final val ASTID = -10 }

case class MethodHandleConst(pc: PC, value: MethodHandle) extends Const {
    final def astID: Int = MethodHandleConst.ASTID
    final def tpe = ObjectType.MethodHandle
    final def cTpe: ComputationalType = ComputationalTypeReference
}
object MethodHandleConst { final val ASTID = -11 }

case class ClassConst(pc: PC, value: ReferenceType) extends SimpleValueConst {
    final def astID: Int = ClassConst.ASTID
    final def tpe = ObjectType.Class
    final def cTpe: ComputationalType = ComputationalTypeReference
}
object ClassConst { final val ASTID = -12 }

case class NullExpr(pc: PC) extends SimpleValueConst {
    final def astID: Int = NullExpr.ASTID
    final def tpe = ObjectType.Object // TODO Should we introduce a fake type such as "java.null"
    final def cTpe: ComputationalType = ComputationalTypeReference
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
    final def astID: Int = BinaryExpr.ASTID
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
    final def astID: Int = PrefixExpr.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        operand.remapIndexes(pcToIndex)
    }
}
object PrefixExpr { final val ASTID = -15 }

case class PrimitiveTypecastExpr(pc: PC, targetTpe: BaseType, operand: Expr) extends Expr {
    final def astID: Int = PrimitiveTypecastExpr.ASTID
    final def cTpe: ComputationalType = targetTpe.computationalType
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        operand.remapIndexes(pcToIndex)
    }
}
object PrimitiveTypecastExpr { final val ASTID = -16 }

case class New(pc: PC, tpe: ObjectType) extends Expr {
    final def astID: Int = New.ASTID
    final def cTpe: ComputationalType = ComputationalTypeReference
}
object New { final val ASTID = -17 }

/**
 *
 * @param pc
 * @param counts Encodes the number of dimensions that are initialized and the size of the
 *               respective dimension.
 * @param tpe The type of the array. The number of dimensions is always `>= count.size`.
 */
case class NewArray(pc: PC, counts: Seq[Expr], tpe: ArrayType) extends Expr {
    final def astID: Int = NewArray.ASTID
    final def cTpe: ComputationalType = ComputationalTypeReference
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        counts.foreach { c ⇒ c.remapIndexes(pcToIndex) }
    }
}
object NewArray { final val ASTID = -18 }

case class ArrayLoad(pc: PC, index: Var, arrayRef: Var) extends Expr {
    final def astID: Int = ArrayLoad.ASTID
    final def cTpe: ComputationalType = ComputationalTypeReference
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        index.remapIndexes(pcToIndex)
        arrayRef.remapIndexes(pcToIndex)
    }
}
object ArrayLoad { final val ASTID = -19 }

case class ArrayLength(pc: PC, arrayRef: Var) extends Expr {
    final def astID: Int = ArrayLength.ASTID
    final def cTpe: ComputationalType = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        arrayRef.remapIndexes(pcToIndex)
    }
}
object ArrayLength { final val ASTID = -20 }

case class GetField(pc: PC, declaringClass: ObjectType, name: String, objRef: Expr) extends Expr {
    final def astID: Int = GetField.ASTID
    final def cTpe: ComputationalType = ComputationalTypeInt
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        objRef.remapIndexes(pcToIndex)
    }
}
object GetField { final val ASTID = -21 }

case class GetStatic(pc: PC, declaringClass: ObjectType, name: String) extends Expr {
    final def astID: Int = GetStatic.ASTID
    final def cTpe: ComputationalType = ComputationalTypeInt
}
object GetStatic { final val ASTID = -22 }

case class Invokedynamic(
        pc:              PC,
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      MethodDescriptor,
        params:          Seq[Expr]
) extends Expr {
    final def astID: Int = Invokedynamic.ASTID
    final def cTpe: ComputationalType = descriptor.returnType.computationalType
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object Invokedynamic { final val ASTID = -23 }

sealed abstract class FunctionCall extends Expr with Call {
    final def cTpe: ComputationalType = descriptor.returnType.computationalType
}

sealed abstract class InstanceFunctionCall extends FunctionCall {
    def receiver: Expr
}

case class NonVirtualFunctionCall(
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr,
        params:         Seq[Expr]
) extends InstanceFunctionCall {
    final def astID: Int = NonVirtualFunctionCall.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object NonVirtualFunctionCall { final val ASTID = -24 }

case class VirtualFunctionCall(
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        receiver:       Expr,
        params:         Seq[Expr]
) extends InstanceFunctionCall {
    final def astID: Int = VirtualFunctionCall.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        receiver.remapIndexes(pcToIndex)
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object VirtualFunctionCall { final val ASTID = -25 }

case class StaticFunctionCall(
        pc:             PC,
        declaringClass: ReferenceType,
        isInterface:    Boolean,
        name:           String,
        descriptor:     MethodDescriptor,
        params:         Seq[Expr]
) extends FunctionCall {
    final def astID: Int = StaticFunctionCall.ASTID
    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        params.foreach { p ⇒ p.remapIndexes(pcToIndex) }
    }
}
object StaticFunctionCall { final val ASTID = -26 }

trait Var extends ValueExpr {

    final def astID: Int = Var.ASTID

    /**
     * A ''human readable'' name of the local variable.
     */
    def name: String

}

object Var {
    final val ASTID = -27
    def unapply(variable: Var): Some[String] = Some(variable.name)
}

//
//
// VAR DEFINITIONS USED BY THE AI BASED TAC
//
//

/**
 * Identifies a variable which has a single static definition/initialization site.
 */
abstract class DUVar[ValueType <: org.opalj.ai.ValuesDomain#DomainValue] extends Var {

    def value: ValueType

    final def cTpe: ComputationalType = value.computationalType

    def updated(cTpe: ComputationalType): Var = throw new UnsupportedOperationException()

}

/**
 * Identifies the single index(pc) of the instruction which initialized
 * the variable. I.e., per method there must be at most one D variable which
 * has the given origin.
 * Initially, the pc of the underlying bytecode instruction is used.
 */
class DVar[ValueType <: org.opalj.ai.ValuesDomain#DomainValue] private (
        private[tac] var origin:   ValueOrigin,
        val value:                 ValueType,
        private[tac] var useSites: IntSet
) extends DUVar[ValueType] {

    assert(useSites ne null)

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        origin = pcToIndex(origin)
        useSites = useSites.map(pcToIndex.apply) // use site are always positive...
    }

    def definedBy: ValueOrigin = origin

    def usedBy: IntSet = useSites

    def name: String = "l"+origin

}

object DVar {

    def apply(
        d: org.opalj.ai.ValuesDomain
    )(
        origin: ValueOrigin, value: d.DomainValue, useSites: IntSet
    ): DVar[d.DomainValue] = {
        new DVar[d.DomainValue](origin, value, useSites)
    }

}

class UVar[ValueType <: org.opalj.ai.ValuesDomain#DomainValue] private (
        val value:                 ValueType,
        private[tac] var defSites: IntSet
) extends DUVar[ValueType] {

    private[tac] override def remapIndexes(pcToIndex: Array[Int]): Unit = {
        defSites = defSites.map { defSite ⇒
            if (defSite >= 0) pcToIndex(defSite) else defSite /* <= it is a parameter */
        }
    }

    def name: String = {
        if (defSites.size == 1) {
            val defSite = defSites.head
            if (defSite < 0)
                "p"+(-defSite)
            else
                "l"+defSites.head
        } else {
            defSites.mkString("l{", ", ", "}")
        }
    }

    def definedBy: IntSet = defSites

}

object UVar {

    def apply(
        d: org.opalj.ai.ValuesDomain
    )(
        value: d.DomainValue, useSites: IntSet
    ): UVar[d.DomainValue] = {
        new UVar[d.DomainValue](value, useSites)
    }

}

