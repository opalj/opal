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
    override implicit val bootstrapMethodType = ClassTag(classOf[br.BootstrapMethod])

    type BootstrapArgument = br.BootstrapArgument
    override implicit val bootstrapArgumentType = ClassTag(classOf[br.BootstrapArgument])

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

    registerAttributesPostProcessor { attributes =>
        val bsmO = attributes collectFirst { case BootstrapMethodTable(bms) => bms }
        if (bsmO.isDefined) {
            val bootstrapMethods = bsmO.get
            bootstrapMethods foreach { bsm =>
                bsm.arguments foreach {
                    case dc: DynamicConstant => dc.fillInBootstrapMethod(bootstrapMethods)
                    case _                   =>
                }
            }
        }
        attributes
    }
}

