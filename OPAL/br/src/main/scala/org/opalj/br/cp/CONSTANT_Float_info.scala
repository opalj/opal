/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant float value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Float_info(value: ConstantFloat) extends CONSTANT_FieldValue_info {

    def this(value: Float) = this(ConstantFloat(value))

    override def tag: Int = ConstantPoolTags.CONSTANT_Float_ID

    override def asConstantFieldValue(cp: Constant_Pool): ConstantFloat = value

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asConstantValue(cp)

}