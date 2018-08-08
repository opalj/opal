/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

/**
 * Template method to read the (Java 7) ''BootstrapMethods'' attribute.
 *
 * '''From the Specification'''
 * The `BootstrapMethods` attribute is a variable-length attribute in the
 * attributes table of a `ClassFile` structure. The `BootstrapMethods` attribute
 * records bootstrap method specifiers referenced by `invokedynamic` instructions.
 */
trait BootstrapMethods_attributeReader extends AttributeReader {

    //
    // ABSTRACT DEFINITIONS
    //

    type BootstrapMethods_attribute >: Null <: Attribute

    type BootstrapMethod
    implicit val BootstrapMethodManifest: ClassTag[BootstrapMethod]

    type BootstrapArgument
    implicit val BootstrapArgumentManifest: ClassTag[BootstrapArgument]

    def BootstrapMethods_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Int,
        bootstrap_methods:    BootstrapMethods,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): BootstrapMethods_attribute

    def BootstrapMethod(
        constant_pool:        Constant_Pool,
        bootstrap_method_ref: Int,
        bootstrap_arguments:  BootstrapArguments
    ): BootstrapMethod

    def BootstrapArgument(
        constant_pool:     Constant_Pool,
        constant_pool_ref: Int
    ): BootstrapArgument

    //
    // IMPLEMENTATION
    //

    type BootstrapMethods = IndexedSeq[BootstrapMethod]

    type BootstrapArguments = IndexedSeq[BootstrapArgument]

    def BootstrapArgument(cp: Constant_Pool, in: DataInputStream): BootstrapArgument = {
        BootstrapArgument(cp, in.readUnsignedShort)
    }

    def BootstrapMethod(cp: Constant_Pool, in: DataInputStream): BootstrapMethod = {
        BootstrapMethod(
            cp,
            in.readUnsignedShort,
            repeat(in.readUnsignedShort) {
                BootstrapArgument(cp, in)
            }
        )
    }

    /**
     * <pre>
     * BootstrapMethods_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 num_bootstrap_methods;
     *  {   u2 bootstrap_method_ref;
     *      u2 num_bootstrap_arguments;
     *      u2 bootstrap_arguments[num_bootstrap_arguments];
     *  } bootstrap_methods[num_bootstrap_methods];
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        as_name_index: Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt
        val num_bootstrap_methods = in.readUnsignedShort
        if (num_bootstrap_methods > 0 || reifyEmptyAttributes) {
            BootstrapMethods_attribute(
                cp,
                attribute_name_index,
                repeat(num_bootstrap_methods) { BootstrapMethod(cp, in) },
                as_name_index,
                as_descriptor_index
            )
        } else
            null
    }

    registerAttributeReader(BootstrapMethodsAttribute.Name → parserFactory())
}
