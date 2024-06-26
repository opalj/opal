/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem

import org.opalj.ide.problem.AllBottomEdgeFunction
import org.opalj.ide.problem.AllTopEdgeFunction
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.IdentityEdgeFunction

/**
 * Edge function to calculate the value of a variable `i` for a statement `val i = a * x + b`
 */
case class LinearCombinationEdgeFunction(a: Int, b: Int)
    extends EdgeFunction[LinearConstantPropagationValue] {
    override def compute(sourceValue: LinearConstantPropagationValue): LinearConstantPropagationValue = {
        sourceValue match {
            case ConstantValue(x)        => ConstantValue(a * x + b)
            case VariableValue if a == 0 => ConstantValue(b)
            case VariableValue           => VariableValue
            case UnknownValue            => UnknownValue
        }
    }

    override def composeWith(
        secondEdgeFunction: EdgeFunction[LinearConstantPropagationValue]
    ): EdgeFunction[LinearConstantPropagationValue] = {
        secondEdgeFunction match {
            case LinearCombinationEdgeFunction(a2, b2) => LinearCombinationEdgeFunction(a2 * a, a2 * b + b2)
            case VariableValueEdgeFunction             => secondEdgeFunction

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
            case LinearCombinationEdgeFunction(a2, b2) if a2 == a && b2 == b => this
            case LinearCombinationEdgeFunction(_, _)                         => VariableValueEdgeFunction

            case VariableValueEdgeFunction => otherEdgeFunction

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => this

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[LinearConstantPropagationValue]): Boolean = {
        otherEdgeFunction == this ||
        (otherEdgeFunction match {
            case LinearCombinationEdgeFunction(a2, b2) => a == a2 && b == b2
            case _                                     => false
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
            case LinearCombinationEdgeFunction(0, _) => secondEdgeFunction
            case LinearCombinationEdgeFunction(_, _) => this
            case _                                   => this
        }
    }
}
