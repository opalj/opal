/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package linear_constant_propagation
package problem

import org.opalj.ide.problem.MeetLattice

/**
 * Lattice used for linear constant propagation.
 *
 * @author Robin Körkemeier
 */
object LinearConstantPropagationLattice extends MeetLattice[LinearConstantPropagationValue] {
    override def top: LinearConstantPropagationValue = UnknownValue

    override def bottom: LinearConstantPropagationValue = VariableValue

    override def meet(
        x: LinearConstantPropagationValue,
        y: LinearConstantPropagationValue
    ): LinearConstantPropagationValue = (x, y) match {
        case (UnknownValue, y)                                  => y
        case (x, UnknownValue)                                  => x
        case (VariableValue, _)                                 => VariableValue
        case (_, VariableValue)                                 => VariableValue
        case (ConstantValue(xc), ConstantValue(yc)) if xc == yc => x
        case _                                                  => VariableValue
    }
}
