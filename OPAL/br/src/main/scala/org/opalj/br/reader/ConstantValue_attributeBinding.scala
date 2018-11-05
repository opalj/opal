/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.ConstantValue_attributeReader

/**
 * @author Michael Eichberg
 */
trait ConstantValue_attributeBinding
    extends ConstantValue_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type ConstantValue_attribute = ConstantFieldValue[_]

    def ConstantValue_attribute(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attributeNameIndex:  Constant_Pool_Index,
        constantValueIndex:  Constant_Pool_Index
    ): ConstantValue_attribute = {
        cp(constantValueIndex).asConstantFieldValue(cp)
    }
}

