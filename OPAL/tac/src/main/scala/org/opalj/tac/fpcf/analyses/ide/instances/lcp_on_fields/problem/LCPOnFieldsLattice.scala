/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import org.opalj.ide.problem.MeetLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationLattice

/**
 * Lattice used for linear constant propagation on fields
 */
object LCPOnFieldsLattice extends MeetLattice[LCPOnFieldsValue] {
    override def top: LCPOnFieldsValue = UnknownValue

    override def bottom: LCPOnFieldsValue = VariableValue

    override def meet(x: LCPOnFieldsValue, y: LCPOnFieldsValue): LCPOnFieldsValue = (x, y) match {
        case (UnknownValue, y)  => y
        case (x, UnknownValue)  => x
        case (VariableValue, _) => VariableValue
        case (_, VariableValue) => VariableValue
        case (ObjectValue(xValues), ObjectValue(yValues)) =>
            val values = xValues.keySet
                .intersect(yValues.keySet)
                .map { fieldName =>
                    fieldName -> LinearConstantPropagationLattice.meet(xValues(fieldName), yValues(fieldName))
                }
                .toMap
            ObjectValue(values)
        case _ => VariableValue
    }
}
