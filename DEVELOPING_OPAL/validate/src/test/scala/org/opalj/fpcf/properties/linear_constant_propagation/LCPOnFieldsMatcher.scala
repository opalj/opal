/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package linear_constant_propagation

import org.opalj.br.AnnotationLike
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.properties.ide.IDEPropertyMatcherMixin
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class ObjectValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ObjectValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ObjectValues")

    private val constantFieldType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ConstantField")
    private val variableValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/VariableField")
    private val unknownValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/UnknownField")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariablePC = getValue(p, singleAnnotationType, a.elementValuePairs, "pc").asIntValue.value

        val expectedConstantValues =
            mapArrayValueExtractStringAndInt(p, a, "constantValues", constantFieldType, "field", "value")

        val expectedVariableValues = mapArrayValueExtractString(p, a, "variableValues", variableValueType, "field")

        val expectedUnknownValues = mapArrayValueExtractString(p, a, "unknownValues", unknownValueType, "field")

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (f: lcp_on_fields.problem.AbstractObjectFact, lcp_on_fields.problem.ObjectValue(values)) =>
                        expectedVariablePC == f.definedAtIndex &&
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
            )
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
                s"Result should contain (${lcp_on_fields.problem.ObjectFact("?", expectedVariablePC)}, ${lcp_on_fields.problem.ObjectValue(expectedValues)})"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class ArrayValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ArrayValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ArrayValues")

    private val constantArrayElementType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ConstantArrayElement")
    private val variableArrayElementType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/VariableArrayElement")
    private val unknownArrayElementType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/UnknownArrayElement")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariablePC =
            getValue(p, singleAnnotationType, a.elementValuePairs, "pc").asIntValue.value

        val expectedConstantElements =
            mapArrayValueExtractIntAndInt(p, a, "constantElements", constantArrayElementType, "index", "value")

        val expectedVariableElements =
            mapArrayValueExtractInt(p, a, "variableElements", variableArrayElementType, "index")

        val expectedUnknownElements = mapArrayValueExtractInt(p, a, "unknownElements", unknownArrayElementType, "index")

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            f: lcp_on_fields.problem.AbstractArrayFact,
                            lcp_on_fields.problem.ArrayValue(initValue, elements)
                        ) =>
                        expectedVariablePC == f.definedAtIndex &&
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
            )
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
                s"Result should contain (${lcp_on_fields.problem.ArrayFact("?", expectedVariablePC)}, ArrayValue(?, $expectedElements)"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.StaticValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class StaticValuesMatcher extends AbstractPropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/StaticValues")

    private val constantFieldType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/ConstantField")
    private val variableValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/VariableField")
    private val unknownValueType = ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/UnknownField")

    override def validateProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val entityObjectType = entity.asInstanceOf[Method].classFile.thisType

        val expectedConstantValues =
            mapArrayValueExtractStringAndInt(p, a, "constantValues", constantFieldType, "field", "value")

        val expectedVariableValues = mapArrayValueExtractString(p, a, "variableValues", variableValueType, "field")

        val expectedUnknownValues = mapArrayValueExtractString(p, a, "unknownValues", unknownValueType, "field")

        if (expectedConstantValues.forall {
                case (fieldName, value) => existsBasicIDEPropertyResult(
                        properties,
                        {
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
                    )
            } &&
            expectedVariableValues.forall { fieldName =>
                existsBasicIDEPropertyResult(
                    properties,
                    {
                        case (
                                f: lcp_on_fields.problem.AbstractStaticFieldFact,
                                lcp_on_fields.problem.StaticFieldValue(v)
                            ) =>
                            f.objectType == entityObjectType && f.fieldName == fieldName &&
                                v == linear_constant_propagation.problem.VariableValue

                        case _ => false
                    }
                )
            } &&
            expectedUnknownValues.forall { fieldName =>
                existsBasicIDEPropertyResult(
                    properties,
                    {
                        case (
                                f: lcp_on_fields.problem.AbstractStaticFieldFact,
                                lcp_on_fields.problem.StaticFieldValue(v)
                            ) =>
                            f.objectType == entityObjectType && f.fieldName == fieldName &&
                                v == linear_constant_propagation.problem.UnknownValue

                        case _ => false
                    }
                )
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
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class VariableValueMatcherLCP extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/VariableValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/VariableValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariablePC =
            getValue(p, singleAnnotationType, a.elementValuePairs, "pc").asIntValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (f: lcp_on_fields.problem.AbstractObjectFact, lcp_on_fields.problem.VariableValue) =>
                        expectedVariablePC == f.definedAtIndex
                    case (f: lcp_on_fields.problem.AbstractArrayFact, lcp_on_fields.problem.VariableValue) =>
                        expectedVariablePC == f.definedAtIndex

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${lcp_on_fields.problem.ObjectFact("?", expectedVariablePC)}, ${lcp_on_fields.problem.VariableValue})!"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.UnknownValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.UnknownValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class UnknownValueMatcherLCP extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/UnknownValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/lcp_on_fields/UnknownValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariablePC =
            getValue(p, singleAnnotationType, a.elementValuePairs, "pc").asIntValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (f: lcp_on_fields.problem.AbstractObjectFact, lcp_on_fields.problem.UnknownValue) =>
                        expectedVariablePC == f.definedAtIndex
                    case (f: lcp_on_fields.problem.AbstractArrayFact, lcp_on_fields.problem.UnknownValue) =>
                        expectedVariablePC == f.definedAtIndex

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${lcp_on_fields.problem.ObjectFact("?", expectedVariablePC)}, ${lcp_on_fields.problem.UnknownValue})!"
            )
        }
    }
}
