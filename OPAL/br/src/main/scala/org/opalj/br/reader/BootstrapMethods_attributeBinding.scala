/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.BootstrapMethods_attributeReader

import scala.reflect.ClassTag

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
    val BootstrapMethodManifest: ClassTag[BootstrapMethod] = implicitly

    type BootstrapArgument = br.BootstrapArgument
    val BootstrapArgumentManifest: ClassTag[BootstrapArgument] = implicitly

    def BootstrapMethods_attribute(
        cp:                 Constant_Pool,
        attributeNameIndex: Int,
        bootstrapMethods:   BootstrapMethods,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
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

