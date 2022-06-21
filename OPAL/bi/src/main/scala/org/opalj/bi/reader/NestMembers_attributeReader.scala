/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillArrayOfInt

/**
 * Generic parser for the ''NestMembers'' attribute (Java 11).
 *
 * '''From the Specification'''
 * The NestMembers attribute is a variable-length attribute in the attributes table of a ClassFile
 * structure. The NestMembers attribute records the classes and interfaces that are authorized to
 * claim membership in the nest hosted by the current class or interface (§5.4.4).
 */
trait NestMembers_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type NestMembers_attribute >: Null <: Attribute

    // CONCEPTUALLY:
    // type ClassesArrayEntry
    // type ClassesArray = <X>Array[ClassesArrayEntry]
    type ClassesArray = Array[Constant_Pool_Index]

    def NestMembers_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        classes_array:        ClassesArray // CONSTANT_Class_info[]
    ): NestMembers_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * NestMembers_attribute {
     *      u2 attribute_name_index;
     *      u4 attribute_length;
     *      u2 number_of_classes;
     *      u2 classes[number_of_classes];
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
        val numberOfClasses = in.readUnsignedShort()
        if (numberOfClasses > 0 || reifyEmptyAttributes) {
            val classesArray = fillArrayOfInt(numberOfClasses) { in.readUnsignedShort() }
            NestMembers_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                classesArray
            )
        } else {
            null
        }
    }: NestMembers_attribute

    registerAttributeReader(NestMembersAttribute.Name -> parserFactory())

}
