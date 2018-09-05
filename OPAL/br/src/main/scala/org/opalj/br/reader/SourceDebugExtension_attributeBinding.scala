/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.SourceDebugExtension_attributeReader

/**
 * Provides the factory method to create a source debug extension attribute.
 *
 * @author Michael Eichberg
 */
trait SourceDebugExtension_attributeBinding
    extends SourceDebugExtension_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type SourceDebugExtension_attribute = br.SourceDebugExtension

    def SourceDebugExtension_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        debug_extension:      Array[Byte]
    ): SourceDebugExtension_attribute = {
        new SourceDebugExtension(debug_extension)
    }

}

