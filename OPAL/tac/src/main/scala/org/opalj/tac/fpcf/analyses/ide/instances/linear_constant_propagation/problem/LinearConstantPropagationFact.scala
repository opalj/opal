/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package linear_constant_propagation
package problem

import org.opalj.ide.problem.IDEFact

/**
 * Type for modeling facts for linear constant propagation.
 *
 * @author Robin Körkemeier
 */
trait LinearConstantPropagationFact extends IDEFact

/**
 * Fact to use as null fact.
 *
 * @author Robin Körkemeier
 */
case object NullFact extends LinearConstantPropagationFact

/**
 * Fact representing a seen variable.
 *
 * @param name the name of the variable (e.g. `lv0`)
 * @param definedAtIndex where the variable is defined (used to uniquely identify a variable/variable fact)
 *
 * @author Robin Körkemeier
 */
case class VariableFact(name: String, definedAtIndex: Int) extends LinearConstantPropagationFact
