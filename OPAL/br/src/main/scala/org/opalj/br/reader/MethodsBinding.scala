/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.MethodsReader

/**
 * @author Michael Eichberg
 */
trait MethodsBinding extends MethodsReader { this: ConstantPoolBinding with AttributeBinding ⇒

    type Method_Info = br.Method

    def Method_Info(
        cp:               Constant_Pool,
        accessFlags:      Int,
        name_index:       Int,
        descriptor_index: Int,
        attributes:       Attributes
    ): Method_Info = {
        Method.unattached(
            accessFlags,
            cp(name_index).asString,
            cp(descriptor_index).asMethodDescriptor,
            attributes
        )
    }
}
