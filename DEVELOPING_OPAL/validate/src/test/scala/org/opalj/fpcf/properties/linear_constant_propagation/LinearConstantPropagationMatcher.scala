/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package linear_constant_propagation

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.properties.ide.IDEPropertyMatcherMixin
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.ConstantValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class ConstantValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/ConstantValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/ConstantValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value
        val expectedVariableValue =
            getValue(p, singleAnnotationType, a.elementValuePairs, "value").asIntValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            linear_constant_propagation.problem.VariableFact(name, _),
                            linear_constant_propagation.problem.ConstantValue(value)
                        ) =>
                        expectedVariableName == name && expectedVariableValue == value

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${linear_constant_propagation.problem.VariableFact(expectedVariableName, 0)}, ${linear_constant_propagation.problem.ConstantValue(expectedVariableValue)})!"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.VariableValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.VariableValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class VariableValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/VariableValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/VariableValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            linear_constant_propagation.problem.VariableFact(name, _),
                            linear_constant_propagation.problem.VariableValue
                        ) =>
                        expectedVariableName == name

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${linear_constant_propagation.problem.VariableFact(expectedVariableName, 0)}, ${linear_constant_propagation.problem.VariableValue})!"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.UnknownValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.UnknownValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class UnknownValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcherMixin {
    override val singleAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/UnknownValue")
    override val containerAnnotationType: ObjectType =
        ObjectType("org/opalj/fpcf/properties/linear_constant_propagation/UnknownValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableName =
            getValue(p, singleAnnotationType, a.elementValuePairs, "variable").asStringValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            linear_constant_propagation.problem.VariableFact(name, _),
                            linear_constant_propagation.problem.UnknownValue
                        ) =>
                        expectedVariableName == name

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${linear_constant_propagation.problem.VariableFact(expectedVariableName, 0)}, ${linear_constant_propagation.problem.UnknownValue})!"
            )
        }
    }
}
