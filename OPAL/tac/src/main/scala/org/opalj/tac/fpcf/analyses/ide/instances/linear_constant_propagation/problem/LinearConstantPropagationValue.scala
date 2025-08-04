/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package linear_constant_propagation
package problem

import org.opalj.ide.problem.IDEValue

/**
 * Type for modeling values for linear constant propagation.
 *
 * @author Robin Körkemeier
 */
trait LinearConstantPropagationValue extends IDEValue

/**
 * Value not known (yet).
 *
 * @author Robin Körkemeier
 */
case object UnknownValue extends LinearConstantPropagationValue

/**
 * A constant value.
 *
 * @author Robin Körkemeier
 */
case class ConstantValue(c: Int) extends LinearConstantPropagationValue

/**
 * Value is variable.
 *
 * @author Robin Körkemeier
 */
case object VariableValue extends LinearConstantPropagationValue
