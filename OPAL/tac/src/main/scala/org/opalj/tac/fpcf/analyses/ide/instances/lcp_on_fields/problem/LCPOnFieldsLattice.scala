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

    override def meet(value1: LCPOnFieldsValue, value2: LCPOnFieldsValue): LCPOnFieldsValue = (value1, value2) match {
        case (UnknownValue, _)  => value2
        case (_, UnknownValue)  => value1
        case (VariableValue, _) => VariableValue
        case (_, VariableValue) => VariableValue

        case (ObjectValue(values1), ObjectValue(values2)) =>
            val values = values1.keySet
                .intersect(values2.keySet)
                .map { fieldName =>
                    fieldName -> LinearConstantPropagationLattice.meet(values1(fieldName), values2(fieldName))
                }
                .toMap
            ObjectValue(values)

        case (ArrayValue(initValue1, elements1), ArrayValue(initValue2, elements2)) =>
            val elements = elements1.keySet
                .union(elements2.keySet)
                .map { index =>
                    index -> LinearConstantPropagationLattice.meet(
                        elements1.getOrElse(index, initValue1),
                        elements2.getOrElse(index, initValue2)
                    )
                }
                .toMap
            ArrayValue(
                LinearConstantPropagationLattice.meet(initValue1, initValue2),
                elements
            )

        case (StaticFieldValue(staticValue1), StaticFieldValue(staticValue2)) =>
            StaticFieldValue(LinearConstantPropagationLattice.meet(staticValue1, staticValue2))

        case _ => VariableValue
    }
}
