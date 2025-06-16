/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Represents a class or an interface.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_Class_info(name_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def tag: Int = ConstantPoolTags.CONSTANT_Class_ID

    override def asClassType(cp: Constant_Pool): ClassType = ClassType(cp(name_index).asString)

    override def asReferenceType(cp: Constant_Pool): ReferenceType = {
        ReferenceType(cp(name_index).asString)
    }

    override def asConstantValue(cp: Constant_Pool): ConstantClass = {
        ConstantClass(asReferenceType(cp))
    }

    override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = asConstantValue(cp)

}
