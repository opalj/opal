/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant integer value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Integer_info(value: ConstantInteger) extends CONSTANT_FieldValue_info {

    def this(value: Int) = this(ConstantInteger(value))

    override def tag: Int = ConstantPoolTags.CONSTANT_Integer_ID

    override def asConstantFieldValue(cp: Constant_Pool): ConstantInteger = value

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asConstantValue(cp)

}
