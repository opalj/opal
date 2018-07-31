/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

/**
 * Used to report errors in the specification itself.
 *
 * @author Michael Eichberg
 */
case class SpecificationError(description: String) extends Exception(description)
