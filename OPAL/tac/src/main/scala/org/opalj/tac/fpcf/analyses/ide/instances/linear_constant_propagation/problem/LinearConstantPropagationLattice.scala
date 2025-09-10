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
        value1: LinearConstantPropagationValue,
        value2: LinearConstantPropagationValue
    ): LinearConstantPropagationValue = (value1, value2) match {
        case (UnknownValue, _)                                                              => value2
        case (_, UnknownValue)                                                              => value1
        case (VariableValue, _)                                                             => VariableValue
        case (_, VariableValue)                                                             => VariableValue
        case (ConstantValue(constant1), ConstantValue(constant2)) if constant1 == constant2 => value1
        case _                                                                              => VariableValue
    }
}
