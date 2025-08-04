/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package linear_constant_propagation

import org.opalj.br.AnnotationLike
import org.opalj.br.ClassType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.properties.ide.IDEPropertyMatcher
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class ConstantValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcher {
    override val singleAnnotationType: ClassType =
        ClassType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/ConstantValue")
    override val containerAnnotationType: ClassType =
        ClassType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/ConstantValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ClassType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableTacIndex =
            getValue(p, singleAnnotationType, a.elementValuePairs, "tacIndex").asIntValue.value
        val expectedVariableValue =
            getValue(p, singleAnnotationType, a.elementValuePairs, "value").asIntValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            linear_constant_propagation.problem.VariableFact(_, definedAtIndex),
                            linear_constant_propagation.problem.ConstantValue(value)
                        ) =>
                        expectedVariableTacIndex == definedAtIndex && expectedVariableValue == value

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${linear_constant_propagation.problem.VariableFact("?", expectedVariableTacIndex)}, ${linear_constant_propagation.problem.ConstantValue(expectedVariableValue)})!"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class VariableValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcher {
    override val singleAnnotationType: ClassType =
        ClassType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/VariableValue")
    override val containerAnnotationType: ClassType =
        ClassType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/VariableValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ClassType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableTacIndex =
            getValue(p, singleAnnotationType, a.elementValuePairs, "tacIndex").asIntValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            linear_constant_propagation.problem.VariableFact(_, definedAtIndex),
                            linear_constant_propagation.problem.VariableValue
                        ) =>
                        expectedVariableTacIndex == definedAtIndex

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${linear_constant_propagation.problem.VariableFact("?", expectedVariableTacIndex)}, ${linear_constant_propagation.problem.VariableValue})!"
            )
        }
    }
}

/**
 * Matcher for [[org.opalj.fpcf.properties.linear_constant_propagation.lcp.UnknownValue]] and
 * [[org.opalj.fpcf.properties.linear_constant_propagation.lcp.UnknownValues]] annotations.
 *
 * @author Robin Körkemeier
 */
class UnknownValueMatcher extends AbstractRepeatablePropertyMatcher with IDEPropertyMatcher {
    override val singleAnnotationType: ClassType =
        ClassType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/UnknownValue")
    override val containerAnnotationType: ClassType =
        ClassType("org/opalj/fpcf/properties/linear_constant_propagation/lcp/UnknownValues")

    override def validateSingleProperty(
        p:          Project[?],
        as:         Set[ClassType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedVariableTacIndex =
            getValue(p, singleAnnotationType, a.elementValuePairs, "tacIndex").asIntValue.value

        if (existsBasicIDEPropertyResult(
                properties,
                {
                    case (
                            linear_constant_propagation.problem.VariableFact(_, definedAtIndex),
                            linear_constant_propagation.problem.UnknownValue
                        ) =>
                        expectedVariableTacIndex == definedAtIndex

                    case _ => false
                }
            )
        ) {
            None
        } else {
            Some(
                s"Result should contain (${linear_constant_propagation.problem.VariableFact("?", expectedVariableTacIndex)}, ${linear_constant_propagation.problem.UnknownValue})!"
            )
        }
    }
}
