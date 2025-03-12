/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package lcp_on_fields

import org.opalj.br.AnnotationLike
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.ide.integration.BasicIDEProperty
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

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
                        case (f: lcp_on_fields.problem.AbstractObjectFact, lcp_on_fields.problem.ObjectValue(values)) =>
                            expectedVariableName == f.name &&
                                expectedConstantValues.forall {
                                    case (fieldName, value) =>
                                        values.get(fieldName) match {
                                            case Some(linear_constant_propagation.problem.ConstantValue(c)) =>
                                                value == c
                                            case _ => false
                                        }
                                } &&
                                expectedVariableValues.forall { fieldName =>
                                    values.get(fieldName) match {
                                        case Some(linear_constant_propagation.problem.VariableValue) => true
                                        case _                                                       => false
                                    }
                                } &&
                                expectedUnknownValues.forall { fieldName =>
                                    values.get(fieldName) match {
                                        case Some(linear_constant_propagation.problem.UnknownValue) => true
                                        case _                                                      => false
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
                    .map { case (fieldName, c) => fieldName -> linear_constant_propagation.problem.ConstantValue(c) }
                    .concat(expectedVariableValues.map { fieldName =>
                        fieldName -> linear_constant_propagation.problem.VariableValue
                    })
                    .concat(expectedUnknownValues.map { fieldName =>
                        fieldName -> linear_constant_propagation.problem.UnknownValue
                    })
                    .toMap
            Some(
                s"Result should contain (${lcp_on_fields.problem.ObjectFact(expectedVariableName, 0)}, ${lcp_on_fields.problem.ObjectValue(expectedValues)})"
            )
        }
    }
}

/**
 * Matcher for [[ArrayValue]] and [[ArrayValues]] annotations
 */
class ArrayValueMatcher extends AbstractRepeatablePropertyMatcher {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/ArrayValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/ArrayValues")

    private val constantArrayElementType = ObjectType("org/opalj/fpcf/properties/lcp_on_fields/ConstantArrayElement")
    private val variableArrayElementType = ObjectType("org/opalj/fpcf/properties/lcp_on_fields/VariableArrayElement")
    private val unknownArrayElementType = ObjectType("org/opalj/fpcf/properties/lcp_on_fields/UnknownArrayElement")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value

        val expectedConstantElements =
            getValue(p, singleAnnotationType, a.elementValuePairs, "constantElements").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedIndex =
                        getValue(p, constantArrayElementType, annotation.elementValuePairs, "index").asIntValue.value
                    val expectedValue =
                        getValue(p, constantArrayElementType, annotation.elementValuePairs, "value").asIntValue.value

                    (expectedIndex, expectedValue)
                }

        val expectedVariableElements =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variableElements").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedIndex =
                        getValue(p, variableArrayElementType, annotation.elementValuePairs, "index").asIntValue.value

                    expectedIndex
                }

        val expectedUnknownElements =
            getValue(p, singleAnnotationType, a.elementValuePairs, "unknownElements").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedIndex =
                        getValue(p, unknownArrayElementType, annotation.elementValuePairs, "index").asIntValue.value

                    expectedIndex
                }

        if (properties.exists {
                case property: BasicIDEProperty[?, ?] =>
                    property.results.exists {
                        case (
                                f: lcp_on_fields.problem.AbstractArrayFact,
                                lcp_on_fields.problem.ArrayValue(initValue, elements)
                            ) =>
                            expectedVariableName == f.name &&
                                expectedConstantElements.forall {
                                    case (index, value) =>
                                        elements.get(index) match {
                                            case Some(linear_constant_propagation.problem.ConstantValue(c)) =>
                                                value == c
                                            case None =>
                                                initValue == linear_constant_propagation.problem.ConstantValue(value)
                                            case _ => false
                                        }
                                } &&
                                expectedVariableElements.forall { index =>
                                    elements.get(index) match {
                                        case Some(linear_constant_propagation.problem.VariableValue) => true
                                        case None =>
                                            initValue == linear_constant_propagation.problem.VariableValue
                                        case _ => false
                                    }
                                } &&
                                expectedUnknownElements.forall { index =>
                                    elements.get(index) match {
                                        case Some(linear_constant_propagation.problem.UnknownValue) => true
                                        case None =>
                                            initValue == linear_constant_propagation.problem.UnknownValue
                                        case _ => false
                                    }
                                }

                        case _ => false
                    }

                case _ => false
            }
        ) {
            None
        } else {
            val expectedElements =
                expectedConstantElements
                    .map { case (index, c) => index -> linear_constant_propagation.problem.ConstantValue(c) }
                    .concat(expectedVariableElements.map { index =>
                        index -> linear_constant_propagation.problem.VariableValue
                    })
                    .concat(expectedUnknownElements.map { index =>
                        index -> linear_constant_propagation.problem.UnknownValue
                    })
                    .toMap
            Some(
                s"Result should contain (${lcp_on_fields.problem.ArrayFact(expectedVariableName, 0)}, ArrayValue(?, $expectedElements)"
            )
        }
    }
}

/**
 * Matcher for [[StaticValues]] annotations
 */
class StaticValuesMatcher extends AbstractPropertyMatcher {
    private val annotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/ObjectValue")

    private val constantValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/ConstantValue")
    private val variableValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/VariableValue")
    private val unknownValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/UnknownValue")

    override def validateProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val entityObjectType = entity.asInstanceOf[Method].classFile.thisType

        val expectedConstantValues =
            getValue(p, annotationType, a.elementValuePairs, "constantValues").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedFieldName =
                        getValue(p, constantValueType, annotation.elementValuePairs, "variable").asStringValue.value
                    val expectedValue =
                        getValue(p, constantValueType, annotation.elementValuePairs, "value").asIntValue.value

                    (expectedFieldName, expectedValue)
                }

        val expectedVariableValues =
            getValue(p, annotationType, a.elementValuePairs, "variableValues").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedFieldName =
                        getValue(p, variableValueType, annotation.elementValuePairs, "variable").asStringValue.value

                    expectedFieldName
                }

        val expectedUnknownValues =
            getValue(p, annotationType, a.elementValuePairs, "unknownValues").asArrayValue.values
                .map { a =>
                    val annotation = a.asAnnotationValue.annotation
                    val expectedFieldName =
                        getValue(p, unknownValueType, annotation.elementValuePairs, "variable").asStringValue.value

                    expectedFieldName
                }

        if (properties.exists {
                case property: BasicIDEProperty[?, ?] =>
                    expectedConstantValues.forall {
                        case (fieldName, value) =>
                            property.results.exists {
                                case (
                                        f: lcp_on_fields.problem.AbstractStaticFieldFact,
                                        lcp_on_fields.problem.StaticFieldValue(v)
                                    ) =>
                                    f.objectType == entityObjectType && f.fieldName == fieldName &&
                                        (v match {
                                            case linear_constant_propagation.problem.ConstantValue(c) => value == c
                                            case _                                                    => false
                                        })

                                case _ => false
                            }
                    } &&
                        expectedVariableValues.forall { fieldName =>
                            property.results.exists {
                                case (
                                        f: lcp_on_fields.problem.AbstractStaticFieldFact,
                                        lcp_on_fields.problem.StaticFieldValue(v)
                                    ) =>
                                    f.objectType == entityObjectType && f.fieldName == fieldName &&
                                        v == linear_constant_propagation.problem.VariableValue

                                case _ => false
                            }
                        } &&
                        expectedUnknownValues.forall { fieldName =>
                            property.results.exists {
                                case (
                                        f: lcp_on_fields.problem.AbstractStaticFieldFact,
                                        lcp_on_fields.problem.StaticFieldValue(v)
                                    ) =>
                                    f.objectType == entityObjectType && f.fieldName == fieldName &&
                                        v == linear_constant_propagation.problem.UnknownValue

                                case _ => false
                            }
                        }

                case _ => false
            }
        ) {
            None
        } else {
            val expectedValues =
                expectedConstantValues
                    .map { case (fieldName, c) => fieldName -> linear_constant_propagation.problem.ConstantValue(c) }
                    .concat(expectedVariableValues.map { fieldName =>
                        fieldName -> linear_constant_propagation.problem.VariableValue
                    })
                    .concat(expectedUnknownValues.map { fieldName =>
                        fieldName -> linear_constant_propagation.problem.UnknownValue
                    })
                    .toMap
            Some(
                s"Result should contain ${expectedValues.map {
                        case (fieldName, value) =>
                            s"(${lcp_on_fields.problem.StaticFieldFact(entityObjectType, fieldName)}, ${lcp_on_fields.problem.StaticFieldValue(value)})"

                    }}"
            )
        }
    }
}

/**
 * Matcher for [[VariableValue]] and [[VariableValues]] annotations
 */
class VariableValueMatcher extends AbstractRepeatablePropertyMatcher {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/VariableValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/VariableValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value

        if (properties.exists {
                case property: BasicIDEProperty[?, ?] =>
                    property.results.exists {
                        case (
                                lcp_on_fields.problem.ObjectFact(name, _),
                                lcp_on_fields.problem.VariableValue
                            ) =>
                            expectedVariableName == name
                        case (
                                lcp_on_fields.problem.ArrayFact(name, _),
                                lcp_on_fields.problem.VariableValue
                            ) =>
                            expectedVariableName == name

                        case _ => false
                    }

                case _ => false
            }
        ) {
            None
        } else {
            Some(
                s"Result should contain (${lcp_on_fields.problem.ObjectFact(expectedVariableName, 0)}, ${lcp_on_fields.problem.VariableValue})!"
            )
        }
    }
}

/**
 * Matcher for [[UnknownValue]] and [[UnknownValues]] annotations
 */
class UnknownValueMatcher extends AbstractRepeatablePropertyMatcher {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/UnknownValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/lcp_on_fields/UnknownValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value

        if (properties.exists {
                case property: BasicIDEProperty[?, ?] =>
                    property.results.exists {
                        case (
                                lcp_on_fields.problem.ObjectFact(name, _),
                                lcp_on_fields.problem.UnknownValue
                            ) =>
                            expectedVariableName == name
                        case (
                                lcp_on_fields.problem.ArrayFact(name, _),
                                lcp_on_fields.problem.UnknownValue
                            ) =>
                            expectedVariableName == name

                        case _ => false
                    }

                case _ => false
            }
        ) {
            None
        } else {
            Some(
                s"Result should contain (${lcp_on_fields.problem.ObjectFact(expectedVariableName, 0)}, ${lcp_on_fields.problem.UnknownValue})!"
            )
        }
    }
}
