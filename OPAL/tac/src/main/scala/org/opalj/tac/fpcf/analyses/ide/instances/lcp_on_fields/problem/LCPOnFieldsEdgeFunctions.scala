/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem

import scala.collection.immutable

import org.opalj.ide.problem.AllBottomEdgeFunction
import org.opalj.ide.problem.AllTopEdgeFunction
import org.opalj.ide.problem.EdgeFunction
import org.opalj.ide.problem.IdentityEdgeFunction
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation
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
            case UnknownValue         => UnknownValue
            case ObjectValue(values2) => ObjectValue((values -- values2.keys) ++ values2)
            case VariableValue        => ObjectValue(values)

            case _ =>
                throw new UnsupportedOperationException(s"Computing $this for $sourceValue is not implemented!")
        }

    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] =
        secondEdgeFunction match {
            case ObjectEdgeFunction(values2) =>
                ObjectEdgeFunction((values -- values2.keys) ++ values2)

            case PutFieldEdgeFunction(fieldName, value) =>
                ObjectEdgeFunction((values - fieldName) + (fieldName -> value))

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => secondEdgeFunction
            case AllBottomEdgeFunction(_) => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] =
        otherEdgeFunction match {
            case ObjectEdgeFunction(values2) =>
                ObjectEdgeFunction(
                    values.keySet
                        .union(values2.keySet)
                        .map { fieldName =>
                            fieldName -> LinearConstantPropagationLattice.meet(
                                values.getOrElse(fieldName, linear_constant_propagation.problem.VariableValue),
                                values2.getOrElse(fieldName, linear_constant_propagation.problem.VariableValue)
                            )
                        }
                        .toMap
                )

            case PutFieldEdgeFunction(fieldName, value) =>
                ObjectEdgeFunction(
                    (values - fieldName).map { case (fieldName2, _) =>
                        fieldName2 -> linear_constant_propagation.problem.VariableValue
                    } +
                        (fieldName -> LinearConstantPropagationLattice.meet(
                            value,
                            values.getOrElse(fieldName, linear_constant_propagation.problem.VariableValue)
                        ))
                )

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => this
            case AllBottomEdgeFunction(_) => otherEdgeFunction

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
            case ObjectEdgeFunction(values) if values.contains(fieldName) =>
                ObjectEdgeFunction((values - fieldName) + (fieldName -> value))
            case ObjectEdgeFunction(values) =>
                ObjectEdgeFunction(values + (fieldName -> value))

            case PutFieldEdgeFunction(fieldName2, _) if fieldName == fieldName2 => secondEdgeFunction
            case PutFieldEdgeFunction(fieldName2, value2) =>
                ObjectEdgeFunction(immutable.Map(fieldName -> value, fieldName2 -> value2))

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => secondEdgeFunction
            case AllBottomEdgeFunction(_) => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        otherEdgeFunction match {
            case ObjectEdgeFunction(values2) =>
                ObjectEdgeFunction(
                    (values2 - fieldName).map { case (fieldName2, _) =>
                        fieldName2 -> linear_constant_propagation.problem.VariableValue
                    } +
                        (fieldName -> LinearConstantPropagationLattice.meet(
                            value,
                            values2.getOrElse(fieldName, linear_constant_propagation.problem.VariableValue)
                        ))
                )

            case PutFieldEdgeFunction(fieldName2, value2) if fieldName == fieldName2 =>
                PutFieldEdgeFunction(fieldName, LinearConstantPropagationLattice.meet(value, value2))
            case PutFieldEdgeFunction(fieldName2, _) =>
                ObjectEdgeFunction(immutable.Map(
                    fieldName -> linear_constant_propagation.problem.VariableValue,
                    fieldName2 -> linear_constant_propagation.problem.VariableValue
                ))

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => this
            case AllBottomEdgeFunction(_) => otherEdgeFunction

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

/**
 * Edge function holding the current array state. An array is represented as an initial value and a collection of
 * elements. The array length is not tracked in this problem definition, thus arbitrary indices can be read and written.
 * The initial value is used as a fallback/default value for elements that are not in the collection of elements yet
 * (will likely be one of `ConstantValue(0)` and `VariableValue`).
 */
class ArrayEdgeFunction(
    val initValue: LinearConstantPropagationValue,
    val elements:  immutable.Map[Int, LinearConstantPropagationValue]
) extends EdgeFunction[LCPOnFieldsValue] {
    override def compute(sourceValue: LCPOnFieldsValue): LCPOnFieldsValue =
        sourceValue match {
            case UnknownValue                      => UnknownValue
            case ArrayValue(initValue2, elements2) => ArrayValue(initValue2, (elements -- elements2.keys) ++ elements2)
            case VariableValue                     => ArrayValue(initValue, elements)

            case _ =>
                throw new UnsupportedOperationException(s"Computing $this for $sourceValue is not implemented!")
        }

    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] =
        secondEdgeFunction match {
            case NewArrayEdgeFunction(_) => secondEdgeFunction

            case ArrayEdgeFunction(initValue2, elements2) =>
                ArrayEdgeFunction(initValue2, (elements -- elements2.keys) ++ elements2)

            case PutElementEdgeFunction(index, value) =>
                index match {
                    case linear_constant_propagation.problem.UnknownValue =>
                        /* In this case it is unknown which indices will be affected */
                        ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
                    case linear_constant_propagation.problem.ConstantValue(i) =>
                        ArrayEdgeFunction(initValue, (elements - i) + (i -> value))
                    case linear_constant_propagation.problem.VariableValue =>
                        /* In this case any index could be affected */
                        ArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)
                }

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => secondEdgeFunction
            case AllBottomEdgeFunction(_) => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] =
        otherEdgeFunction match {
            case ArrayEdgeFunction(initValue2, elements2) =>
                ArrayEdgeFunction(
                    LinearConstantPropagationLattice.meet(initValue, initValue2),
                    elements.keySet.union(elements2.keySet)
                        .map { index =>
                            index -> LinearConstantPropagationLattice.meet(
                                elements.getOrElse(index, initValue),
                                elements2.getOrElse(index, initValue2)
                            )
                        }
                        .toMap
                )

            case PutElementEdgeFunction(index, value) =>
                index match {
                    case linear_constant_propagation.problem.UnknownValue =>
                        ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
                    case linear_constant_propagation.problem.ConstantValue(i) =>
                        ArrayEdgeFunction(
                            linear_constant_propagation.problem.VariableValue,
                            immutable.Map(i -> LinearConstantPropagationLattice.meet(
                                value,
                                elements.getOrElse(i, initValue)
                            ))
                        )
                    case linear_constant_propagation.problem.VariableValue =>
                        ArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)
                }

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => this
            case AllBottomEdgeFunction(_) => otherEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }

    override def equalTo(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case ArrayEdgeFunction(initValue2, elements2) => initValue == initValue2 && elements == elements2
                case _                                        => false
            })
}

object ArrayEdgeFunction {
    def apply(
        initValue: LinearConstantPropagationValue,
        elements:  immutable.Map[Int, LinearConstantPropagationValue] = immutable.Map.empty
    ): ArrayEdgeFunction = {
        new ArrayEdgeFunction(initValue, elements)
    }

    def unapply(arrayEdgeFunction: ArrayEdgeFunction): Some[(
        LinearConstantPropagationValue,
        immutable.Map[Int, LinearConstantPropagationValue]
    )] = Some((arrayEdgeFunction.initValue, arrayEdgeFunction.elements))
}

/**
 * Edge function for initializing an array
 */
case class NewArrayEdgeFunction(
    override val initValue: LinearConstantPropagationValue = linear_constant_propagation.problem.ConstantValue(0)
) extends ArrayEdgeFunction(initValue, immutable.Map.empty) {
    override def toString: String = s"NewArrayEdgeFunction($initValue)"
}

/**
 * Edge function modeling the effect of writing an element of an array
 */
case class PutElementEdgeFunction(
    index: LinearConstantPropagationValue,
    value: LinearConstantPropagationValue
) extends EdgeFunction[LCPOnFieldsValue] {
    override def compute(sourceValue: LCPOnFieldsValue): LCPOnFieldsValue = {
        sourceValue match {
            case UnknownValue => UnknownValue
            case ArrayValue(initValue, elements) =>
                index match {
                    case linear_constant_propagation.problem.UnknownValue =>
                        ArrayValue(linear_constant_propagation.problem.UnknownValue, immutable.Map.empty)
                    case linear_constant_propagation.problem.ConstantValue(i) =>
                        ArrayValue(initValue, (elements - i) + (i -> value))
                    case linear_constant_propagation.problem.VariableValue =>
                        ArrayValue(linear_constant_propagation.problem.VariableValue, immutable.Map.empty)
                }
            case VariableValue => VariableValue

            case _ =>
                throw new UnsupportedOperationException(s"Computing $this for $sourceValue is not implemented!")
        }
    }

    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        secondEdgeFunction match {
            case NewArrayEdgeFunction(_) => secondEdgeFunction

            case ArrayEdgeFunction(initValue, elements) =>
                index match {
                    case linear_constant_propagation.problem.UnknownValue =>
                        ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
                    case linear_constant_propagation.problem.ConstantValue(i) =>
                        ArrayEdgeFunction(
                            initValue,
                            (elements - i) + (i -> LinearConstantPropagationLattice.meet(
                                value,
                                elements.getOrElse(i, initValue)
                            ))
                        )
                    case linear_constant_propagation.problem.VariableValue =>
                        ArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)
                }

            case PutElementEdgeFunction(index2, _) if index == index2 => secondEdgeFunction
            case PutElementEdgeFunction(_, _) =>
                ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
                    .composeWith(this).composeWith(secondEdgeFunction)

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => secondEdgeFunction
            case AllBottomEdgeFunction(_) => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        otherEdgeFunction match {
            case ArrayEdgeFunction(initValue, elements) =>
                index match {
                    case linear_constant_propagation.problem.UnknownValue =>
                        ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
                    case linear_constant_propagation.problem.ConstantValue(i) =>
                        ArrayEdgeFunction(
                            linear_constant_propagation.problem.VariableValue,
                            immutable.Map(i -> LinearConstantPropagationLattice.meet(
                                value,
                                elements.getOrElse(i, initValue)
                            ))
                        )
                    case linear_constant_propagation.problem.VariableValue =>
                        ArrayEdgeFunction(linear_constant_propagation.problem.VariableValue)
                }

            case PutElementEdgeFunction(index2, value2) =>
                PutElementEdgeFunction(
                    LinearConstantPropagationLattice.meet(index, index2),
                    LinearConstantPropagationLattice.meet(value, value2)
                )

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => this
            case AllBottomEdgeFunction(_) => otherEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case PutElementEdgeFunction(index2, value2) => index == index2 && value == value2
                case _                                      => false
            })
}

/**
 * Edge function modeling the effect of when a static field gets written
 */
case class PutStaticFieldEdgeFunction(
    value: LinearConstantPropagationValue
) extends EdgeFunction[LCPOnFieldsValue] {
    override def compute(sourceValue: LCPOnFieldsValue): LCPOnFieldsValue = {
        StaticFieldValue(value)
    }

    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        secondEdgeFunction match {
            case PutStaticFieldEdgeFunction(_) => secondEdgeFunction

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => secondEdgeFunction
            case AllBottomEdgeFunction(_) => secondEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        otherEdgeFunction match {
            case PutStaticFieldEdgeFunction(value2) =>
                PutStaticFieldEdgeFunction(
                    LinearConstantPropagationLattice.meet(value, value2)
                )

            case IdentityEdgeFunction()   => this
            case AllTopEdgeFunction(_)    => this
            case AllBottomEdgeFunction(_) => otherEdgeFunction

            case _ =>
                throw new UnsupportedOperationException(s"Meeting $this with $otherEdgeFunction is not implemented!")
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): Boolean =
        (otherEdgeFunction eq this) ||
            (otherEdgeFunction match {
                case PutStaticFieldEdgeFunction(value2) => value == value2
                case _                                  => false
            })
}

/**
 * Edge function for cases where a value is unknown
 */
object UnknownValueEdgeFunction extends AllTopEdgeFunction[LCPOnFieldsValue](UnknownValue) {
    override def composeWith(
        secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]
    ): EdgeFunction[LCPOnFieldsValue] = {
        secondEdgeFunction match {
            case ObjectEdgeFunction(_)                  => secondEdgeFunction
            case PutFieldEdgeFunction(fieldName, value) => ObjectEdgeFunction(immutable.Map(fieldName -> value))
            case ArrayEdgeFunction(_, _)                => secondEdgeFunction
            case PutElementEdgeFunction(_, _)           => ArrayEdgeFunction(linear_constant_propagation.problem.UnknownValue)
            case PutStaticFieldEdgeFunction(_)          => secondEdgeFunction

            case VariableValueEdgeFunction => secondEdgeFunction
            case UnknownValueEdgeFunction  => secondEdgeFunction

            case IdentityEdgeFunction() => this
            case AllTopEdgeFunction(_)  => this

            case _ =>
                throw new UnsupportedOperationException(s"Composing $this with $secondEdgeFunction is not implemented!")
        }
    }

    override def meetWith(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        otherEdgeFunction match {
            case AllTopEdgeFunction(_)  => this
            case IdentityEdgeFunction() => this
            case _                      => otherEdgeFunction
        }
    }

    override def equalTo(otherEdgeFunction: EdgeFunction[LCPOnFieldsValue]): Boolean = {
        otherEdgeFunction eq this
    }

    override def toString: String = "UnknownValueEdgeFunction()"
}

/**
 * Edge function for cases where a value is variable
 */
object VariableValueEdgeFunction extends AllBottomEdgeFunction[LCPOnFieldsValue](VariableValue) {
    override def composeWith(secondEdgeFunction: EdgeFunction[LCPOnFieldsValue]): EdgeFunction[LCPOnFieldsValue] = {
        secondEdgeFunction match {
            case NewObjectEdgeFunction => secondEdgeFunction
            case _                     => this
        }
    }

    override def toString: String = "VariableValueEdgeFunction()"
}
