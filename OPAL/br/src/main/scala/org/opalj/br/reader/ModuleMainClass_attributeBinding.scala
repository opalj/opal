/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.ModuleMainClass_attributeReader

/**
 * The factory method to create the `ModuleMainClass` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait ModuleMainClass_attributeBinding
    extends ModuleMainClass_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type ModuleMainClass_attribute = ModuleMainClass

    /**
     * @param main_class_index Reference to a CONSTANT_Class_info.
     */
    def ModuleMainClass_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        main_class_index:     Constant_Pool_Index // CONSTANT_Class_info
    ): ModuleMainClass_attribute = {
        new ModuleMainClass(cp(main_class_index).asObjectType(cp))
    }

}

