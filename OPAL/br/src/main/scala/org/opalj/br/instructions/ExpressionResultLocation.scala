/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Characterizes the result of evaluating an expression with respect to the place where
 * the result is stored.
 *
 * @author Michael Eichberg
 */
sealed abstract class ExpressionResultLocation

case object Stack extends ExpressionResultLocation

case class Register(index: Int) extends ExpressionResultLocation

case object NoExpression extends ExpressionResultLocation
