/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.SourceFile_attributeReader

/**
 * The factory method to create the source file attribute.
 *
 * @author Michael Eichberg
 */
trait SourceFile_attributeBinding
    extends SourceFile_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type SourceFile_attribute = br.SourceFile

    def SourceFile_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        sourcefile_index:     Constant_Pool_Index
    ): SourceFile_attribute = {
        new SourceFile_attribute(cp(sourcefile_index).asString)
    }

}

