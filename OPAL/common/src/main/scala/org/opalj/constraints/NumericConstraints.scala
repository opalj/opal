/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package constraints

import scala.annotation.switch

/**
 * Enumeration of all possible relations/constraints between two arbitrary numeric values.
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
     * E.g., `inverse(&gt;) = &lt;`; `x < y === y > x`.
     */
    def inverse(relation: Value): Value = {
        (relation.id: @switch) match {
            case LT => >
            case LE => >=
            case GT => <
            case GE => <=
            case EQ => ==
            case NE => !=
        }
    }

    /**
     * Calculates the constraint that is in effect if both constraints need to be
     * satisfied at the same time. E.g., the result of combining "less than" with
     * "equal or less than" is "less than". However, the combination of "less than"
     * with "greater than" would throw an exception.
     *
     * @note This a '''narrowing''' operation.
     *
     * @return The combined constraint.
     */
    @throws[IncompatibleNumericConstraints]("if the combination doesn't make sense")
    def combine(c1: Value, c2: Value): Value = {
        (c1.id: @switch) match {
            case LT =>
                (c2.id: @switch) match {
                    case LT => <
                    case LE => <
                    case NE => <
                    case _  => throw new IncompatibleNumericConstraints(c1, c2)
                }

            case LE =>
                (c2.id: @switch) match {
                    case LT => <
                    case LE => <=
                    case GE => ==
                    case EQ => ==
                    case NE => <
                    case _  => throw new IncompatibleNumericConstraints(c1, c2)
                }

            case GT =>
                (c2.id: @switch) match {
                    case GT => >
                    case GE => >
                    case NE => >
                    case _  => throw new IncompatibleNumericConstraints(c1, c2)
                }

            case GE =>
                (c2.id: @switch) match {
                    case LE => ==
                    case GT => >
                    case GE => >=
                    case EQ => ==
                    case NE => >
                    case _  => throw new IncompatibleNumericConstraints(c1, c2)
                }

            case EQ =>
                (c2.id: @switch) match {
                    case LE => ==
                    case GE => ==
                    case EQ => ==
                    case _  => throw new IncompatibleNumericConstraints(c1, c2)
                }

            case NE =>
                (c2.id: @switch) match {
                    case LT => <
                    case LE => <
                    case GT => >
                    case GE => >
                    case NE => !=
                    case _  => throw new IncompatibleNumericConstraints(c1, c2)
                }
        }
    }

    /**
     * Joins the given constraints. I.e., returns the constraint that still has to
     * hold if either `c1` or `c2` holds. E.g., the result of joining "<" with "==" with
     * is "<=".
     *
     * @note This is a '''widening''' operation.
     */
    def join(c1: Value, c2: Value): Option[Value] = {
        (c1.id: @switch) match {
            case LT =>
                (c2.id: @switch) match {
                    case LT => Some(<)
                    case LE => Some(<=)
                    case GT => Some(!=)
                    case GE => None
                    case NE => Some(!=)
                    case EQ => Some(<=)
                }

            case LE =>
                (c2.id: @switch) match {
                    case LT => Some(<=)
                    case LE => Some(<=)
                    case GT => None
                    case GE => None
                    case NE => None
                    case EQ => Some(<=)
                }

            case GT =>
                (c2.id: @switch) match {
                    case LT => Some(!=)
                    case LE => None
                    case GT => Some(>)
                    case GE => Some(>=)
                    case NE => Some(!=)
                    case EQ => Some(>=)
                }

            case GE =>
                (c2.id: @switch) match {
                    case LT => None
                    case LE => None
                    case GT => Some(>=)
                    case GE => Some(>=)
                    case NE => None
                    case EQ => Some(>=)
                }

            case EQ =>
                (c2.id: @switch) match {
                    case LT => Some(<=)
                    case LE => Some(<=)
                    case GT => Some(>=)
                    case GE => Some(>=)
                    case NE => None
                    case EQ => Some(==)
                }

            case NE =>
                (c2.id: @switch) match {
                    case LT => Some(!=)
                    case LE => None
                    case GT => Some(!=)
                    case GE => None
                    case NE => Some(!=)
                    case EQ => None
                }
        }
    }
}
