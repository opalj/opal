/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.VisibilityModifier

/**
 * We treat as a source element every entity that can be referred to
 * by other class files.
 *
 * @author Michael Eichberg
 */
trait ConcreteSourceElement extends SourceElement {

    def accessFlags: Int

    def hasFlags(accessFlags: Int): Boolean = (accessFlags & this.accessFlags) == accessFlags

    def visibilityModifier: Option[VisibilityModifier] = VisibilityModifier.get(accessFlags)

}
