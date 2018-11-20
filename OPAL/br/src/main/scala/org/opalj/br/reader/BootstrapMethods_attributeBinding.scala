/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.BootstrapMethods_attributeReader

/**
 * Final bindings and factory methods for the ''BoostrapMethods'' attribute.
 *
 * @author Michael Eichberg
 */
trait BootstrapMethods_attributeBinding
    extends BootstrapMethods_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type BootstrapMethods_attribute = BootstrapMethodTable

    type BootstrapMethod = br.BootstrapMethod

    type BootstrapArgument = br.BootstrapArgument

    def BootstrapMethods_attribute(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attributeNameIndex:  Int,
        bootstrapMethods:    BootstrapMethods
    ): BootstrapMethods_attribute = {
        new BootstrapMethodTable(bootstrapMethods)
    }

    def BootstrapMethod(
        cp:                 Constant_Pool,
        bootstrapMethodRef: Int,
        bootstrapArguments: BootstrapArguments
    ): BootstrapMethod = {
        new BootstrapMethod(cp(bootstrapMethodRef).asMethodHandle(cp), bootstrapArguments)
    }

    def BootstrapArgument(
        cp:                Constant_Pool,
        constantPoolIndex: Int
    ): BootstrapArgument = {
        cp(constantPoolIndex).asBootstrapArgument(cp)
    }
}

