/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import org.opalj.control.fillArrayOfInt

import java.io.DataInputStream

/**
 * Generic parser for the ''PermittedSubclasses'' attribute (Java 17).
 *
 * '''From the Specification'''
 * The PermittedSubclasses attribute is a variable-length attribute in the attributes table of a
 * ClassFile structure (§4.1). The PermittedSubclasses attribute records the classes and interfaces that
 * are authorized to directly extend or implement the current class or interface (§5.3.5).
 *
 * @author Julius Naeumann
 */
trait PermittedSubclasses_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type PermittedSubclasses_attribute >: Null <: Attribute

    type PermittedSubclassesArray = Array[Constant_Pool_Index]

    def PermittedSubclasses_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        classes:              PermittedSubclassesArray
    ): PermittedSubclasses_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     *
     * PermittedSubclasses_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 number_of_classes;
     *  u2 classes[number_of_classes];
     * }
     *
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
        /*val attribute_length =*/ in.readInt()
        val classes_count = in.readUnsignedShort
        if (classes_count > 0 || reifyEmptyAttributes) {
            val classesArray = fillArrayOfInt(classes_count) { in.readUnsignedShort() }
            PermittedSubclasses_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                classesArray
            )
        } else {
            null
        }
    }: PermittedSubclasses_attribute

    registerAttributeReader(PermittedSubclassesAttribute.Name -> parserFactory())

}
