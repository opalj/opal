/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Encapsulates the bounds related to a specific entity/property kind.
 *
 * @author Michael Eichberg
 */
case class PropertyBounds[P <: Property](lb: P, ub: P)
