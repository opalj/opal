/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_PROTECTED
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.VisibilityModifier
import org.opalj.bi.ACC_SYNTHETIC

/**
 * Abstractions over the common properties of class members (Methods and Fields).
 *
 * @author Michael Eichberg
 */
trait ClassMember extends ConcreteSourceElement {

    final def isPublic: Boolean = (ACC_PUBLIC.mask & accessFlags) != 0

    final def isProtected: Boolean = (ACC_PROTECTED.mask & accessFlags) != 0

    final def isPrivate: Boolean = (ACC_PRIVATE.mask & accessFlags) != 0

    final def hasDefaultVisibility: Boolean = VisibilityModifier.hasDefaultVisibility(accessFlags)

    final def isPackagePrivate: Boolean = hasDefaultVisibility

    final def isStatic: Boolean = (ACC_STATIC.mask & accessFlags) != 0

    final def isNotStatic: Boolean = (ACC_STATIC.mask & accessFlags) == 0

    final def isFinal: Boolean = (ACC_FINAL.mask & accessFlags) != 0

    final def isNotFinal: Boolean = !isFinal

    /**
     * `True` if the `Synthetic` access flag or attribute is used.
     */
    final override def isSynthetic: Boolean = {
        super.isSynthetic || (ACC_SYNTHETIC.mask & accessFlags) != 0
    }

    /**
     * The simple name of this class member (method or field).
     */
    def name: String
}
/**
 * Defines an extractor method for class members.
 *
 * @author Michael Eichberg
 */
object ClassMember {

    def unapply(classMember: ClassMember): Option[Int] = Some(classMember.accessFlags)

}
