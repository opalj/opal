/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem

import org.opalj.ide.problem.AllBottomEdgeFunction
import org.opalj.ide.problem.AllTopEdgeFunction
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.IdentityEdgeFunction

/**
 * Edge function to calculate the value of a variable `i` for a statement `val i = a * x + b`
 */
case class LinearCombinationEdgeFunction(
        a: Int,
        b: Int,
        c: LinearConstantPropagationValue = LinearConstantPropagationLattice.top
) extends EdgeFunction[LinearConstantPropagationValue] {
    override def compute(sourceValue: LinearConstantPropagationValue): LinearConstantPropagationValue = {
        LinearConstantPropagationLattice.meet(
            sourceValue match {
                case ConstantValue(x)        => ConstantValue(a * x + b)
                case VariableValue if a == 0 => ConstantValue(b)
                case VariableValue           => VariableValue
                case UnknownValue            => UnknownValue
            },
            c
        )
    }

    override def composeWith(
        secondEdgeFunction: EdgeFunction[LinearConstantPropagationValue]
    ): EdgeFunction[LinearConstantPropagationValue] = {
        secondEdgeFunction match {
            case LinearCombinationEdgeFunction(a2, b2, c2) =>
                LinearCombinationEdgeFunction(
                    a2 * a,
                    a2 * b + b2,
                    LinearConstantPropagationLattice.meet(
                        c match {
                            case UnknownValue          => UnknownValue
                            case ConstantValue(cValue) => ConstantValue(a2 * cValue + b2)
                            case VariableValue         => VariableValue
                        },
                        c2
                    )
                )

            case VariableValueEdgeFunction => secondEdgeFunction
            case UnknownValueEdgeFunction => secondEdgeFunction

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    override def meetWith(
        otherEdgeFunction: EdgeFunction[LinearConstantPropagationValue]
    ): EdgeFunction[LinearConstantPropagationValue] = {
        otherEdgeFunction match {
            case LinearCombinationEdgeFunction(a2, b2, c2) if a2 == a && b2 == b =>
                LinearCombinationEdgeFunction(a, b, LinearConstantPropagationLattice.meet(c, c2))
            case LinearCombinationEdgeFunction(a2, b2, c2) if a2 != a && (b - b2) % (a2 - a) == 0 =>
                val cNew = LinearConstantPropagationLattice.meet(
                    ConstantValue(a * ((b - b2) / (a2 - a)) + b),
                    LinearConstantPropagationLattice.meet(c, c2)
                )
                cNew match {
                    case VariableValue => VariableValueEdgeFunction
                    case _             => LinearCombinationEdgeFunction(a, b, cNew)
                }
            case LinearCombinationEdgeFunction(_, _, _) =>
                VariableValueEdgeFunction

            case VariableValueEdgeFunction => otherEdgeFunction
            case UnknownValueEdgeFunction => this

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => this

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[LinearConstantPropagationValue]): Boolean = {
        (otherEdgeFunction eq this) ||
        (otherEdgeFunction match {
            case LinearCombinationEdgeFunction(a2, b2, c2) => a == a2 && b == b2 && c == c2
            case _                                         => false
        })
    }
}

/**
 * Edge function for a variable that is definitely not constant
 */
object VariableValueEdgeFunction extends AllBottomEdgeFunction[LinearConstantPropagationValue](VariableValue) {
    override def composeWith(
        secondEdgeFunction: EdgeFunction[LinearConstantPropagationValue]
    ): EdgeFunction[LinearConstantPropagationValue] = {
        secondEdgeFunction match {
            case LinearCombinationEdgeFunction(0, _, _) => secondEdgeFunction
            case LinearCombinationEdgeFunction(_, _, _) => this
            case _                                      => this
        }
    }
}

/**
 * Edge function for variables whose value is unknown
 */
object UnknownValueEdgeFunction extends AllTopEdgeFunction[LinearConstantPropagationValue](UnknownValue) {
    override def composeWith(
        secondEdgeFunction: EdgeFunction[LinearConstantPropagationValue]
    ): EdgeFunction[LinearConstantPropagationValue] = {
        secondEdgeFunction match {
            case LinearCombinationEdgeFunction(_, _, _) => secondEdgeFunction

            case VariableValueEdgeFunction => secondEdgeFunction
            case UnknownValueEdgeFunction => secondEdgeFunction

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }
}
