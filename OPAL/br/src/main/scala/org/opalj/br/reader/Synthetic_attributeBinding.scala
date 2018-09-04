/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Synthetic_attributeReader

/**
 * Represents Java's Synthetic attribute.
 *
 * @author Michael Eichberg
 */
trait Synthetic_attributeBinding
    extends Synthetic_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Synthetic_attribute = br.Attribute // ... possible, but useless: br.Synthetic.type

    def Synthetic_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index
    ): br.Attribute = {
        br.Synthetic
    }

}

