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
 * Type for modeling values for linear constant propagation
 */
trait LinearConstantPropagationValue extends IDEValue

/**
 * Value not known (yet)
 */
case object UnknownValue extends LinearConstantPropagationValue

/**
 * A constant value
 */
case class ConstantValue(c: Int) extends LinearConstantPropagationValue

/**
 * Value is variable
 */
case object VariableValue extends LinearConstantPropagationValue
