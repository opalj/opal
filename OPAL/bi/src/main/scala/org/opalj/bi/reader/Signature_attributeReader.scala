/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.log.OPALLogger

/**
 * Implements the template method to read signature attributes.
 *
 * The Signature attribute is an optional attribute in the
 * attributes table of a ClassFile, field_info or method_info structure.
 */
trait Signature_attributeReader extends AttributeReader with ClassFileReaderConfiguration {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Signature_attribute >: Null <: Attribute

    /**
     * Returns `true` if an exception is to be raised if an invalid signature is found.
     * The default is to just log the invalid signature and to otherwise ignore it.
     *
     * This method is intended to be overridden.
     *
     * @note This method was primarily introduced because we found many class files with
     *       invalid signatures AND the JVM also handles this case gracefully!
     *
     * @return `false`.
     */
    def throwIllegalArgumentException: Boolean = false

    /**
     * Creates a `Signature_attribute`.
     *
     * '''From the Specification'''
     *
     * The constant pool entry at signature_index must be a CONSTANT_Utf8_info
     * structure representing either a class signature, if this signature
     * attribute is an attribute of a ClassFile structure, a method type
     * signature, if this signature is an attribute of a method_info structure,
     * or a field type signature otherwise.
     */
    def Signature_attribute(
        constant_pool:        Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        signature_index:      Constant_Pool_Index
    ): Signature_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * The Signature attribute is an optional attribute in the
     * attributes table of a ClassFile, field_info or method_info structure.
     *
     * <pre>
     * Signature_attribute {
     *    u2 attribute_name_index;
     *    u4 attribute_length;
     *    u2 signature_index;
     * }
     * </pre>
     *
     * Given that the Java Reflection API has extensive exception handling support
     * for handling wrong signatures, we at least provide support for the case
     * where the signature is syntactically invalid. In this case the attribute
     * is skipped.
     */
    private[this] def parser(
        cp:                   Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): Signature_attribute = {
        /*val attribute_length =*/ in.readInt
        val signature_index = in.readUnsignedShort
        try {
            Signature_attribute(
                cp, ap, ap_name_index, ap_descriptor_index, attribute_name_index, signature_index
            )
        } catch {
            case iae: IllegalArgumentException =>
                OPALLogger.error(
                    "parsing bytecode",
                    s"skipping ${ap.toString().toLowerCase()} signature: "+iae.getMessage
                )
                if (throwIllegalArgumentException) throw iae else null
        }
    }

    registerAttributeReader(SignatureAttribute.Name -> parser)
}
