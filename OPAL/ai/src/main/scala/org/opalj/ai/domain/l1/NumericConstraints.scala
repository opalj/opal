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

import scala.collection.BitSet

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br.{ ComputationalType, ComputationalTypeInt }
import org.opalj.br.instructions.Instruction

/**
 * Enumeration of all possible relations/constraints between two arbitrary integer values.
 *
 * @author Michael Eichberg
 */
object NumericConstraints extends Enumeration(1) {

    final val LT = 1
    final val < : Value = Value(LT, "<")
    final val LE = 2
    final val <= : Value = Value(LE, "<=")

    final val GT = 3
    final val > : Value = Value(GT, ">")
    final val GE = 4
    final val >= : Value = Value(GE, ">=")

    final val EQ = 5
    final val == : Value = Value(EQ, "==")
    final val NE = 6
    final val != : Value = Value(NE, "!=")

    nextId = 7

    /**
     * Returns the relation when we swap the operands.
     *
     * E.g., `inverse(x ? y) = x ?' y`
     */
    def inverse(relation: Value): Value = {
        (relation.id: @scala.annotation.switch) match {
            case LT ⇒ >
            case LE ⇒ >=
            case GT ⇒ <
            case GE ⇒ <=
            case EQ ⇒ ==
            case NE ⇒ !=
        }
    }

    /**
     * Calculates the constraint that is in effect if both constraints need to be
     * satisfied at the same time.
     *
     * @note This a '''narrowing''' operation.
     * @return The combined constraint.
     * @throws IncompatibleConstraints exception if the combination of the constraints
     *      doesn't make sense.
     */
    def combine(c1: Value, c2: Value): Value = {
        (c1.id: @scala.annotation.switch) match {
            case LT ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ <
                    case LE ⇒ <
                    case NE ⇒ <
                    case _  ⇒ throw IncompatibleNumericConstraints(c1, c2)
                }

            case LE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ <
                    case LE ⇒ <=
                    case GE ⇒ ==
                    case EQ ⇒ ==
                    case NE ⇒ <
                    case _  ⇒ throw IncompatibleNumericConstraints(c1, c2)
                }

            case GT ⇒
                (c2.id: @scala.annotation.switch) match {
                    case GT ⇒ >
                    case GE ⇒ >
                    case NE ⇒ >
                    case _  ⇒ throw IncompatibleNumericConstraints(c1, c2)
                }

            case GE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LE ⇒ ==
                    case GT ⇒ >
                    case GE ⇒ >=
                    case EQ ⇒ ==
                    case NE ⇒ >
                    case _  ⇒ throw IncompatibleNumericConstraints(c1, c2)
                }

            case EQ ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LE ⇒ ==
                    case GE ⇒ ==
                    case EQ ⇒ ==
                    case _  ⇒ throw IncompatibleNumericConstraints(c1, c2)
                }

            case NE ⇒
                (c2.id: @scala.annotation.switch) match {
                    case LT ⇒ <
                    case LE ⇒ <
                    case GT ⇒ >
                    case GE ⇒ >
                    case NE ⇒ !=
                    case _  ⇒ throw IncompatibleNumericConstraints(c1, c2)
                }
        }
    }

    /**
     * Joins the given constraints. I.e., returns the constraint that still has to
     * hold if either `c1` or `c2` holds.
     *
     * @note This is a '''widening''' operation.
     */
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

