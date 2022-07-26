/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant long value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Long_info(value: ConstantLong) extends CONSTANT_FieldValue_info {

    def this(value: Long) = this(ConstantLong(value))

    override def tag: Int = ConstantPoolTags.CONSTANT_Long_ID

    override def asConstantFieldValue(cp: Constant_Pool): ConstantLong = value

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asConstantValue(cp)

}