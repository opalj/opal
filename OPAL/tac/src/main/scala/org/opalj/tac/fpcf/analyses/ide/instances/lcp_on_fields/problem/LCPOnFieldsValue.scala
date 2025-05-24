/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package lcp_on_fields
package problem

import scala.collection.immutable

import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue

/**
 * Type for modeling values for linear constant propagation on fields.
 *
 * @author Robin Körkemeier
 */
trait LCPOnFieldsValue extends IDEValue

/**
 * Value not known (yet).
 *
 * @author Robin Körkemeier
 */
case object UnknownValue extends LCPOnFieldsValue

/**
 * Value representing the state of an object.
 *
 * @author Robin Körkemeier
 */
case class ObjectValue(values: immutable.Map[String, LinearConstantPropagationValue]) extends LCPOnFieldsValue {
    override def toString: String =
        s"ObjectValue(${values.toSeq.sortBy(_._1).map { case (fieldName, value) => s"$fieldName -> $value" }.mkString(", ")})"
}

/**
 * Value representing the state of an array.
 *
 * @author Robin Körkemeier
 */
case class ArrayValue(
    initValue: LinearConstantPropagationValue,
    elements:  immutable.Map[Int, LinearConstantPropagationValue]
) extends LCPOnFieldsValue {
    override def toString: String =
        s"ArrayValue($initValue, ${elements.toSeq.sortBy(_._1).map { case (index, value) => s"$index -> $value" }.mkString(", ")})"
}

/**
 * Value representing the value of a static field.
 *
 * @author Robin Körkemeier
 */
case class StaticFieldValue(
    value: LinearConstantPropagationValue
) extends LCPOnFieldsValue

/**
 * Value is variable (not really used currently, mainly for completeness).
 *
 * @author Robin Körkemeier
 */
case object VariableValue extends LCPOnFieldsValue
