/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

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
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type BootstrapMethods_attribute >: Null <: Attribute

    type BootstrapMethod <: AnyRef
    implicit val bootstrapMethodType: ClassTag[BootstrapMethod] // TODO: Replace in Scala 3 with `type BootstrapMethod: ClassTag`
    type BootstrapMethods = ArraySeq[BootstrapMethod]

    type BootstrapArgument <: AnyRef
    implicit val bootstrapArgumentType: ClassTag[BootstrapArgument] // TODO: Replace in Scala 3 with `type BootstrapArgument: ClassTag`
    type BootstrapArguments = ArraySeq[BootstrapArgument]

    def BootstrapMethods_attribute(
        constant_pool:        Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Int,
        bootstrap_methods:    BootstrapMethods
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

    def BootstrapArgument(cp: Constant_Pool, in: DataInputStream): BootstrapArgument = {
        BootstrapArgument(cp, in.readUnsignedShort)
    }

    def BootstrapMethod(cp: Constant_Pool, in: DataInputStream): BootstrapMethod = {
        BootstrapMethod(
            cp,
            in.readUnsignedShort,
            fillArraySeq(in.readUnsignedShort) {
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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt
        val num_bootstrap_methods = in.readUnsignedShort
        if (num_bootstrap_methods > 0 || reifyEmptyAttributes) {
            BootstrapMethods_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                fillArraySeq(num_bootstrap_methods) { BootstrapMethod(cp, in) }
            )
        } else {
            null
        }
    }

    registerAttributeReader(BootstrapMethodsAttribute.Name -> parserFactory())
}
