/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a method type.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_MethodType_info(
        descriptorIndex: Constant_Pool_Index
) extends Constant_Pool_Entry {

    def methodDescriptor(cp: Constant_Pool): MethodDescriptor = {
        cp(descriptorIndex).asMethodDescriptor
    }

    override def tag: Int = ConstantPoolTags.CONSTANT_MethodType_ID

    override def asConstantValue(cp: Constant_Pool): MethodDescriptor = methodDescriptor(cp)

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = methodDescriptor(cp)
}