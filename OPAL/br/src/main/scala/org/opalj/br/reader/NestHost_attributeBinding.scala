/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.NestHost_attributeReader

/**
 * The factory method to create the `NestHost` attribute (Java 11).
 *
 * @author Dominik Helm
 */
trait NestHost_attributeBinding
    extends NestHost_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type NestHost_attribute = NestHost

    /**
     * @param host_class_index Reference to a CONSTANT_Class_info.
     */
    def NestHost_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        host_class_index:     Constant_Pool_Index // CONSTANT_Class_info
    ): NestHost_attribute = {
        new NestHost(cp(host_class_index).asObjectType(cp))
    }

}

