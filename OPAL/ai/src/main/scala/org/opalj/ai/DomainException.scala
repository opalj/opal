/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * An exception related to a computation in a specific domain occurred.
 * This exception is intended to be used if the exception occurred inside the `Domain`.
 *
 * @author Michael Eichberg
 */
case class DomainException(message: String) extends AIException(message)

