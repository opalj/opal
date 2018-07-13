/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * Encapsulates the bounds of a property.
 */
case class PropertyBounds[P <: Property](lb: P, ub: P)
