/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a constant double value.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Double_info(value: ConstantDouble) extends CONSTANT_FieldValue_info {

    def this(value: Double) = this(ConstantDouble(value))

    override def tag: Int = ConstantPoolTags.CONSTANT_Double_ID

    override def asConstantFieldValue(cp: Constant_Pool): ConstantDouble = value

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asConstantValue(cp)

}