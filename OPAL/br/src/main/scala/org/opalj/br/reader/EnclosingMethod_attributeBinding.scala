/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.EnclosingMethod_attributeReader

/**
 *
 * @author Michael Eichberg
 */
trait EnclosingMethod_attributeBinding
    extends EnclosingMethod_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type EnclosingMethod_attribute = br.EnclosingMethod

    def EnclosingMethod_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        class_index:          Constant_Pool_Index,
        method_index:         Constant_Pool_Index
    ): EnclosingMethod_attribute = {

        if (method_index == 0)
            new EnclosingMethod_attribute(cp(class_index).asObjectType(cp), None, None)
        else {
            val nameAndType = cp(method_index).asNameAndType
            new EnclosingMethod_attribute(
                cp(class_index).asObjectType(cp),
                Some(nameAndType.name(cp)),
                Some(nameAndType.methodDescriptor(cp))
            )
        }
    }
}

