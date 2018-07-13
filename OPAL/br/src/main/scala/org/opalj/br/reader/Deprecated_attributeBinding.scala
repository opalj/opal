/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Deprecated_attributeReader

/**
 * @author Michael Eichberg
 */
trait Deprecated_attributeBinding
    extends Deprecated_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Deprecated_attribute = br.Attribute

    def Deprecated_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index
    ): br.Attribute = {
        br.Deprecated
    }

}

