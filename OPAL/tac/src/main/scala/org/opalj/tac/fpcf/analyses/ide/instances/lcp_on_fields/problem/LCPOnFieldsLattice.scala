/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package lcp_on_fields
package problem

import org.opalj.ide.problem.MeetLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationLattice

/**
 * Lattice used for linear constant propagation on fields.
 *
 * @author Robin Körkemeier
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

        case (ArrayValue(xInitValue, xElements), ArrayValue(yInitValue, yElements)) =>
            val elements = xElements.keySet
                .union(yElements.keySet)
                .map { index =>
                    index -> LinearConstantPropagationLattice.meet(
                        xElements.getOrElse(index, xInitValue),
                        yElements.getOrElse(index, yInitValue)
                    )
                }
                .toMap
            ArrayValue(
                LinearConstantPropagationLattice.meet(xInitValue, yInitValue),
                elements
            )

        case (StaticFieldValue(xValue), StaticFieldValue(yValue)) =>
            StaticFieldValue(LinearConstantPropagationLattice.meet(xValue, yValue))

        case _ => VariableValue
    }
}
