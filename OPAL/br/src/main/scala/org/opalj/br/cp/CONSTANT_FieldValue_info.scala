/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

/**
 * Represents a constant field value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
trait CONSTANT_FieldValue_info extends Constant_Pool_Entry {

    final override def asConstantValue(cp: Constant_Pool): ConstantFieldValue[_] = {
        asConstantFieldValue(cp)
    }

}