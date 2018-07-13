/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Unknown_attributeReader

/**
 * "Factory" to create unknown attributes which are used to represent class
 * file attributes that are not understood by this framework.
 *
 * @author Michael Eichberg
 */
trait Unknown_attributeBinding
    extends Unknown_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Unknown_attribute = br.UnknownAttribute

    def Unknown_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        info:                 Array[Byte]
    ): Unknown_attribute = {
        new Unknown_attribute(cp(attribute_name_index).asString, info)
    }

}

