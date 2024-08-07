/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.lcp_on_fields

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractRepeatablePropertyMatcher
import org.opalj.ide.integration.BasicIDEProperty
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.{problem => LCPOnFieldsProblem}
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.{problem => LCPProblem}

/**
 * Matcher for [[ObjectValue]] and [[ObjectValues]] annotations
 */
class ObjectValueMatcher extends AbstractRepeatablePropertyMatcher {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/ObjectValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/ObjectValues")

    private val constantValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/ConstantValue")
    private val variableValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/VariableValue")
    private val unknownValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/UnknownValue")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value

        val expectedConstantValues =
            getValue(p, singleAnnotationType, a.elementValuePairs, "constantValues").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedFieldName =
                        getValue(p, constantValueType, annotation.elementValuePairs, "variable").asStringValue.value
                    val expectedValue =
                        getValue(p, constantValueType, annotation.elementValuePairs, "value").asIntValue.value

                    (expectedFieldName, expectedValue)
                }

        val expectedVariableValues =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variableValues").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedFieldName =
                        getValue(p, variableValueType, annotation.elementValuePairs, "variable").asStringValue.value

                    expectedFieldName
                }

        val expectedUnknownValues =
            getValue(p, singleAnnotationType, a.elementValuePairs, "unknownValues").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedFieldName =
                        getValue(p, unknownValueType, annotation.elementValuePairs, "variable").asStringValue.value

                    expectedFieldName
                }

        if (properties.exists {
                case property: BasicIDEProperty[?, ?] =>
                    property.results.exists {
                        case (f: LCPOnFieldsProblem.AbstractObjectFact, LCPOnFieldsProblem.ObjectValue(values)) =>
                            expectedVariableName == f.name &&
                                expectedConstantValues.forall {
                                    case (fieldName, value) =>
                                        values.get(fieldName) match {
                                            case Some(LCPProblem.ConstantValue(c)) => value == c
                                            case _                                 => false
                                        }
                                } &&
                                expectedVariableValues.forall { fieldName =>
                                    values.get(fieldName) match {
                                        case Some(LCPProblem.VariableValue) => true
                                        case _                              => false
                                    }
                                } &&
                                expectedUnknownValues.forall { fieldName =>
                                    values.get(fieldName) match {
                                        case Some(LCPProblem.UnknownValue) => true
                                        case _                             => false
                                    }
                                }

                        case _ => false
                    }

                case _ => false
            }
        ) {
            None
        } else {
            val expectedValues =
                expectedConstantValues
                    .map { case (fieldName, c) => fieldName -> LCPProblem.ConstantValue(c) }
                    .concat(expectedVariableValues.map { fieldName => fieldName -> LCPProblem.VariableValue })
                    .concat(expectedUnknownValues.map { fieldName => fieldName -> LCPProblem.UnknownValue })
                    .toMap
            Some(
                s"Result should contain (${LCPOnFieldsProblem.ObjectFact(expectedVariableName, 0)}, ${LCPOnFieldsProblem.ObjectValue(expectedValues)})"
            )
        }
    }
}
