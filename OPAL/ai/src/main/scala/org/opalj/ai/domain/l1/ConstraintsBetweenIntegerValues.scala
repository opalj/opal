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
package ai
package domain
package l1

import java.util.IdentityHashMap

import scala.collection.mutable.WeakHashMap

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.{ ComputationalType, ComputationalTypeInt }

object Constraints extends Enumeration(1) {

    final val LT = 1
    val < = Value(LT, "<")
    final val LE = 2
    val <= = Value(LE, "<=")

    final val GT = 3
    val > = Value(GT, ">")
    final val GE = 4
    val >= = Value(GE, ">=")

    final val EQ = 5
    val == = Value(EQ, "==")
    final val NE = 6
    val != = Value(NE, "!=")

    nextId = 7

    def inverse(constraint: Value): Value = {
        (constraint.id: @scala.annotation.switch) match {
            case LT ⇒ >=
            case LE ⇒ >
            case GT ⇒ <=
            case GE ⇒ <
            case EQ ⇒ !=
            case NE ⇒ ==
        }
    }

    def combine(c1: Value, c2: Value): Value = {
        (c1.id: @scala.annotation.switch) match {
            case LT ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ <
                    case NE ⇒ <
                    case _  ⇒ throw IncompatibleConstraints(c1, c2)
                }

            case LE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ <
                    case LE ⇒ <=
                    case EQ ⇒ ==
                    case NE ⇒ <
                    case _  ⇒ throw IncompatibleConstraints(c1, c2)
                }

            case GT ⇒
                (c2.id: @scala.annotation.switch) match {
                    case GT ⇒ >
                    case NE ⇒ >
                    case _  ⇒ throw IncompatibleConstraints(c1, c2)
                }

            case GE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case GT ⇒ >
                    case GE ⇒ >=
                    case EQ ⇒ ==
                    case NE ⇒ >
                    case _  ⇒ throw IncompatibleConstraints(c1, c2)
                }

            case EQ ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LE ⇒ ==
                    case GE ⇒ ==
                    case EQ ⇒ ==
                    case _  ⇒ throw IncompatibleConstraints(c1, c2)
                }

            case NE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ <
                    case LE ⇒ <
                    case GT ⇒ >
                    case GE ⇒ >
                    case NE ⇒ !=
                    case _  ⇒ throw IncompatibleConstraints(c1, c2)
                }
        }
    }

    def join(c1: Value, c2: Value): Option[Value] = {
        (c1.id: @scala.annotation.switch) match {
            case LT ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ Some(<)
                    case LE ⇒ Some(<=)
                    case GT ⇒ Some(!=)
                    case GE ⇒ None
                    case NE ⇒ Some(!=)
                    case EQ ⇒ Some(<=)
                }

            case LE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ Some(<=)
                    case LE ⇒ Some(<=)
                    case GT ⇒ None
                    case GE ⇒ None
                    case NE ⇒ None
                    case EQ ⇒ Some(<=)
                }

            case GT ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ Some(!=)
                    case LE ⇒ None
                    case GT ⇒ Some(>)
                    case GE ⇒ Some(>=)
                    case NE ⇒ Some(!=)
                    case EQ ⇒ Some(>=)
                }

            case GE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ None
                    case LE ⇒ None
                    case GT ⇒ Some(>=)
                    case GE ⇒ Some(>=)
                    case NE ⇒ None
                    case EQ ⇒ Some(>=)
                }

            case EQ ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ Some(<=)
                    case LE ⇒ Some(<=)
                    case GT ⇒ Some(>=)
                    case GE ⇒ Some(>=)
                    case NE ⇒ None
                    case EQ ⇒ Some(==)
                }

            case NE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ Some(!=)
                    case LE ⇒ None
                    case GT ⇒ Some(!=)
                    case GE ⇒ None
                    case NE ⇒ Some(!=)
                    case EQ ⇒ None
                }
        }
    }
}

case class IncompatibleConstraints(
    constraint1: Constraints.Value,
    constraint2: Constraints.Value)
        extends Exception(s"incompatible: $constraint1 and $constraint2")

/**
 *
 * @author Michael Eichberg
 */
trait ConstraintsBetweenIntegerValues extends CoreDomainFunctionality with IntegerRangeValues {
    domain: JoinStabilization with IdentityBasedAliasBreakUpDetection with Configuration with VMLevelExceptionsFactory ⇒

    type Constraint = Constraints.Value

    private[this] val constraints =
        new IdentityHashMap[IntegerLikeValue, IdentityHashMap[IntegerLikeValue, Constraint]]()

    private[this] def putConstraint(
        v1: IntegerLikeValue,
        v2: IntegerLikeValue,
        c: Constraint): Unit = {

        require(v1 ne v2)

        var m = constraints.get(v1)
        if (m == null) {
            m = new IdentityHashMap()
            constraints.put(v1, m)
            m.put(v2, c)
        } else {
            val old_c = m.get(v2)
            if (old_c == null)
                m.put(v2, c)
            else
                m.put(v2, Constraints.combine(old_c, c))
        }

    }

    private[this] def addConstraint(
        v1: IntegerLikeValue,
        v2: IntegerLikeValue,
        c: Constraint): Unit = {
        putConstraint(v1, v2, c)
        putConstraint(v2, v1, Constraints.inverse(c))

        val cs = (scala.collection.JavaConversions.mapAsScalaMap(constraints).map { e ⇒
            val (v1, v2c) = e
            val jv2c = scala.collection.JavaConversions.mapAsScalaMap(v2c)
            for ((v2, c) ← jv2c) yield s"$v1 [#${System.identityHashCode(v1).toHexString}], $v2 [#${System.identityHashCode(v2).toHexString}] => $c"
        }).flatten
        println(cs.mkString("Constraints:\n\t", "\n\t", ""))
    }

    private[this] def addConstraint(
        v1: DomainValue,
        v2: DomainValue,
        c: Constraint): Unit = {
        addConstraint(
            v1.asInstanceOf[IntegerLikeValue], v2.asInstanceOf[IntegerLikeValue],
            c)
    }

    private[this] def getConstraint(
        v1: IntegerLikeValue,
        v2: IntegerLikeValue): Option[Constraint] = {
        val m = constraints.get(v1)
        if (m == null)
            None
        else {
            Option(m.get(v2))
        }
    }

    private[this] def getConstraint(
        v1: DomainValue,
        v2: DomainValue): Option[Constraint] = {
        getConstraint(v1.asInstanceOf[IntegerLikeValue], v2.asInstanceOf[IntegerLikeValue])
    }

    private[this] val updatedValues = new IdentityHashMap[DomainValue, DomainValue]

    abstract override def updateMemoryLayout(
        oldValue: DomainValue,
        newValue: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        oldValue match {
            case iv: IntegerLikeValue ⇒ updatedValues.put(oldValue, newValue)
        }

        super.updateMemoryLayout(oldValue, newValue, operands, locals)
    }

    def currentValue(value: DomainValue): DomainValue = {
        val updatedValue = this.updatedValues.get(value)
        if (updatedValue != null)
            updatedValue
        else
            value
    }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO INTEGER VALUES
    //
    // -----------------------------------------------------------------------------------

    abstract override def intAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer = {
        super.intAreEqual(pc, value1, value2) match {
            case Unknown ⇒
                val constraint = getConstraint(value1, value2)
                if (constraint.isDefined)
                    constraint.get match {
                        case Constraints.!= ⇒ No
                        case Constraints.>  ⇒ No
                        case Constraints.<  ⇒ No
                        case Constraints.== ⇒ Yes
                        case _              ⇒ Unknown
                    }
                else
                    Unknown
            case answer ⇒
                answer
        }
    }

    override def intIsLessThan(pc: PC, left: DomainValue, right: DomainValue): Answer = {
        super.intAreEqual(pc, left, right) match {
            case Unknown ⇒
                val constraint = getConstraint(left, right)
                if (constraint.isDefined)
                    constraint.get match {
                        case Constraints.>  ⇒ No
                        case Constraints.>= ⇒ No
                        case Constraints.<  ⇒ Yes
                        case Constraints.== ⇒ No
                        case _              ⇒ Unknown
                    }
                else
                    Unknown
            case answer ⇒
                answer
        }
    }

    override def intIsLessThanOrEqualTo(pc: PC, left: DomainValue, right: DomainValue): Answer = {
        super.intIsLessThanOrEqualTo(pc, left, right) match {
            case Unknown ⇒
                val constraint = getConstraint(left, right)
                if (constraint.isDefined)
                    constraint.get match {
                        case Constraints.>  ⇒ No
                        case Constraints.<  ⇒ Yes
                        case Constraints.<= ⇒ Yes
                        case Constraints.== ⇒ Yes
                        case _              ⇒ Unknown
                    }
                else
                    Unknown
            case answer ⇒
                answer
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF CONSTRAINTS
    //
    // -----------------------------------------------------------------------------------

    abstract override def intEstablishValue(
        pc: PC,
        theValue: Int,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        val result = super.intEstablishValue(pc, theValue, value, operands, locals)
        updatedValues.clear
        result
    }

    abstract override def intEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        val result = super.intEstablishAreEqual(pc, value1, value2, operands, locals)
        updatedValues.clear
        result
    }

    override def intEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {
        val result =
            super.intEstablishAreNotEqual(pc, value1, value2, operands, locals)

        addConstraint(
            currentValue(value1),
            currentValue(value2),
            Constraints.!=)

        updatedValues.clear
        result
    }

    override def intEstablishIsLessThan(
        pc: PC,
        left: DomainValue,
        right: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        val result =
            super.intEstablishIsLessThan(pc, left, right, operands, locals)

        addConstraint(
            currentValue(left),
            currentValue(right),
            Constraints.<)

        updatedValues.clear
        result
    }

    override def intEstablishIsLessThanOrEqualTo(
        pc: PC,
        left: DomainValue,
        right: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = {

        val result =
            super.intEstablishIsLessThanOrEqualTo(pc, left, right, operands, locals)

        addConstraint(
            currentValue(left),
            currentValue(right),
            Constraints.<=)

        updatedValues.clear
        result
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //    override def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
    //        (value1, value2) match {
    //            case (IntegerRange(lb1, ub1), IntegerRange(lb2, ub2)) ⇒
    //                // to identify overflows we simply do the "add" on long values
    //                // and check afterwards
    //                val lb = lb1.toLong + lb2.toLong
    //                val ub = ub1.toLong + ub2.toLong
    //                if (lb < Int.MinValue || ub > Int.MaxValue)
    //                    IntegerValue(pc)
    //                else
    //                    IntegerRange(lb.toInt, ub.toInt)
    //            case _ ⇒
    //                // we have to create a new instance... even if we just add "0"
    //                IntegerValue(pc)
    //        }
    //    }

    //
    //    override def iinc(pc: PC, value: DomainValue, increment: Int): DomainValue = {
    //        value match {
    //            case IntegerRange(lb, ub) ⇒
    //                val newLB = lb.toLong + increment.toLong
    //                val newUB = ub.toLong + increment.toLong
    //                if (newLB < Int.MinValue || newUB > Int.MaxValue)
    //                    IntegerValue(pc)
    //                else
    //                    IntegerRange(newLB.toInt, newUB.toInt)
    //            case _ ⇒
    //                IntegerValue(pc)
    //        }
    //    }
    //
    //    override def isub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
    //        if (left eq right)
    //            return IntegerRange(0, 0)
    //
    //        (left, right) match {
    //            case (IntegerRange(llb, lub), IntegerRange(rlb, rub)) ⇒
    //                // to identify overflows we simply do the "add" on long values
    //                // and check afterwards
    //                val lb = llb.toLong - rub.toLong
    //                val ub = lub.toLong - rlb.toLong
    //                if (lb < Int.MinValue || ub > Int.MaxValue)
    //                    IntegerValue(pc)
    //                else
    //                    IntegerRange(lb.toInt, ub.toInt)
    //            case _ ⇒
    //                // we have to create a new instance... even if we just subtract "0"
    //                IntegerValue(pc)
    //        }
    //    }
    //
    //    override def idiv(
    //        pc: PC,
    //        numerator: DomainValue,
    //        denominator: DomainValue): IntegerValueOrArithmeticException = {
    //        denominator match {
    //            case IntegerRange(1, 1) ⇒
    //                ComputedValue(numerator.asInstanceOf[IntegerLikeValue].newInstance)
    //            case IntegerRange(lb, ub) if lb > 0 || ub < 0 ⇒
    //                // no div by "0"
    //                ComputedValue(IntegerValue(pc))
    //            case _ ⇒
    //                if (throwArithmeticExceptions)
    //                    ComputedValueOrException(
    //                        IntegerValue(pc), ArithmeticException(pc))
    //                else
    //                    ComputedValue(IntegerValue(pc))
    //        }
    //    }
    //
    //    override def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
    //        value1 match {
    //            case IntegerRange(lb1, ub1) ⇒
    //                if (lb1 == 0 && ub1 == 0) IntegerRange(0, 0)
    //                else value2 match {
    //                    case IntegerRange(lb2, ub2) ⇒
    //                        // to identify overflows we simply do the "mul" on long values
    //                        // and check afterwards
    //                        val lb1l = lb1.toLong
    //                        val ub1l = ub1.toLong
    //                        val lb2l = lb2.toLong
    //                        val ub2l = ub2.toLong
    //                        val ub =
    //                            Math.max(lb1l * lb2l, ub1l * ub2l)
    //                        val lb =
    //                            Math.min(
    //                                Math.min(
    //                                    Math.min(lb1l * lb2l, ub1l * ub2l),
    //                                    ub1l * lb2l),
    //                                lb1l * ub2l)
    //
    //                        if (lb < Int.MinValue || ub > Int.MaxValue)
    //                            IntegerValue(pc)
    //                        else
    //                            IntegerRange(lb.toInt, ub.toInt)
    //                    case _ ⇒
    //                        IntegerValue(pc)
    //                }
    //
    //            case _ ⇒
    //                value2 match {
    //                    case IntegerRange(0, 0) ⇒ IntegerRange(0, 0)
    //                    case _ ⇒
    //                        IntegerValue(pc)
    //                }
    //        }
    //    }
    //
    //    override def irem(
    //        pc: PC,
    //        left: DomainValue,
    //        right: DomainValue): IntegerValueOrArithmeticException = {
    //        right match {
    //            case IntegerRange(rightLB, rightUB) if rightLB > 0 || rightUB < 0 ⇒
    //                // no div by "0"
    //                ComputedValue(IntegerValue(pc))
    //            case _ ⇒
    //                if (throwArithmeticExceptions)
    //                    ComputedValueOrException(
    //                        IntegerValue(pc), ArithmeticException(pc))
    //                else
    //                    ComputedValue(IntegerValue(pc))
    //        }
    //    }
    //
    //    override def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    override def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
    //        IntegerValue(pc)
    //
    //    //
    //    // TYPE CONVERSION INSTRUCTIONS
    //    //
    //
    //    override def i2b(pc: PC, value: DomainValue): DomainValue =
    //        value match {
    //            case IntegerRange(lb, ub) if lb >= Byte.MinValue && ub <= Byte.MaxValue ⇒
    //                value
    //            case _ ⇒
    //                IntegerRange(Byte.MinValue, Byte.MaxValue)
    //        }
    //
    //    override def i2c(pc: PC, value: DomainValue): DomainValue =
    //        value match {
    //            case IntegerRange(lb, ub) if lb >= Char.MinValue && ub <= Char.MaxValue ⇒
    //                value
    //            case _ ⇒
    //                IntegerRange(Char.MinValue, Char.MaxValue)
    //        }
    //
    //    override def i2s(pc: PC, value: DomainValue): DomainValue =
    //        value match {
    //            case IntegerRange(lb, ub) if lb >= Short.MinValue && ub <= Short.MaxValue ⇒
    //                value
    //            case _ ⇒
    //                IntegerRange(Short.MinValue, Short.MaxValue)
    //        }
}

