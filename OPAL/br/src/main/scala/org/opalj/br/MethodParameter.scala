/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.ACC_MANDATED
import org.opalj.bi.ACC_FINAL

/**
 * The description of a method parameter. If `name` is `None` a formal parameter is described.
 *
 * @author Michael Eichberg
 */
case class MethodParameter(
        name:        Option[String],
        accessFlags: Int
) {

    final def isSynthetic: Boolean = (ACC_SYNTHETIC.mask & accessFlags) != 0

    final def isFinal: Boolean = (ACC_FINAL.mask & accessFlags) != 0

    final def isMandated: Boolean = (ACC_MANDATED.mask & accessFlags) != 0

}
