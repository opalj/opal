/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

import org.opalj.bi.ConstantPoolTags

/**
 * Is used by the invokedynamic instruction to specify a bootstrap method, the dynamic
 * invocation name, the argument and return types of the call, and optionally, a
 * sequence of additional constants called static arguments to the bootstrap method.
 *
 * @param bootstrapMethodAttributeIndex This is an index into the bootstrap table.
 *    Since the bootstrap table is a class level attribute it is only possible
 *    to resolve this reference after loading the entire class file (class level
 *    attributes are loaded last).
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
case class CONSTANT_InvokeDynamic_info(
        bootstrapMethodAttributeIndex: Int,
        nameAndTypeIndex:              Constant_Pool_Index
) extends Constant_Pool_Entry {

    override def asInvokeDynamic: this.type = this

    override def tag: Int = ConstantPoolTags.CONSTANT_InvokeDynamic_ID

    def methodName(cp: Constant_Pool): String = cp(nameAndTypeIndex).asNameAndType.name(cp)

    def methodDescriptor(cp: Constant_Pool): MethodDescriptor = {
        cp(nameAndTypeIndex).asNameAndType.methodDescriptor(cp)
    }

}