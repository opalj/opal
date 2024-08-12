/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable

import org.opalj.ide.problem.AllTopEdgeFunction
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.IdentityEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationLattice
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue

/**
 * Edge function holding the current object state (in form of its field-value mapping)
 */
case class ObjectEdgeFunction(
    values: immutable.Map[String, LinearConstantPropagationValue]
) extends EdgeFunction[LCPOnFieldsValue] {
    override def compute(sourceValue: LCPOnFieldsValue): LCPOnFieldsValue =
        sourceValue match {
            case UnknownValue => UnknownValue
            case ObjectValue(_) =>
                throw new UnsupportedOperationException(s"Computing $this for $sourceValue is not implemented!")
            case VariableValue => ObjectValue(values)
        }

    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] =
        secondEdgeFunction match {
            case ObjectEdgeFunction(values2) =>
                ObjectEdgeFunction((values -- values2.keys) ++ values2)

            case PutFieldEdgeFunction(fieldName, value) =>
                ObjectEdgeFunction((values - fieldName) + (fieldName -> value))

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] =
        otherEdgeFunction match {
            case ObjectEdgeFunction(values2) =>
                ObjectEdgeFunction(
                    values.keySet
                        .intersect(values2.keySet)
                        .map { fieldName =>
                            fieldName -> LinearConstantPropagationLattice.meet(values(fieldName), values2(fieldName))
                        }
                        .toMap
                )

            case PutFieldEdgeFunction(_, _) =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => this

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }

    override def equalTo(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case ObjectEdgeFunction(values2) => values == values2
                case _                           => false
            })
}

/**
 * Edge function for initializing an object
 */
object NewObjectEdgeFunction extends ObjectEdgeFunction(immutable.Map.empty) {
    override def toString: String = "NewObjectEdgeFunction()"
}

/**
 * Edge function modeling the effect of writing the field of an object
 */
case class PutFieldEdgeFunction(
    fieldName: String,
    value:     LinearConstantPropagationValue
) extends EdgeFunction[LCPOnFieldsValue] {
    override def compute(sourceValue: LCPOnFieldsValue): LCPOnFieldsValue = {
        sourceValue match {
            case UnknownValue        => UnknownValue
            case ObjectValue(values) => ObjectValue((values - fieldName) + (fieldName -> value))
            case VariableValue       => VariableValue
        }
    }

    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        secondEdgeFunction match {
            case ObjectEdgeFunction(values) if values.contains(fieldName) => secondEdgeFunction
            case ObjectEdgeFunction(values) =>
                ObjectEdgeFunction(values + (fieldName -> value))

            case PutFieldEdgeFunction(fieldName2, _) if fieldName == fieldName2 => secondEdgeFunction
            case PutFieldEdgeFunction(fieldName2, value2) =>
                ObjectEdgeFunction(immutable.Map(fieldName -> value, fieldName2 -> value2))

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        otherEdgeFunction match {
            case ObjectEdgeFunction(_) =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")

            case PutFieldEdgeFunction(fieldName2, value2) if fieldName == fieldName2 =>
                PutFieldEdgeFunction(fieldName, LinearConstantPropagationLattice.meet(value, value2))
            case PutFieldEdgeFunction(_, _) =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => this

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case PutFieldEdgeFunction(fieldName2, value2) => fieldName == fieldName2 && value == value2
                case _                                        => false
            })
}
