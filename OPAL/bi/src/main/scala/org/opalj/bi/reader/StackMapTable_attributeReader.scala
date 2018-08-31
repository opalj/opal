/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillRefArray
import org.opalj.collection.immutable.RefArray

/**
 * Implementation of a template method to read in the StackMapTable attribute.
 *
 * @author Michael Eichberg
 */
trait StackMapTable_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type StackMapTable_attribute >: Null <: Attribute

    type StackMapFrame <: AnyRef
    type StackMapFrames = RefArray[StackMapFrame]

    def StackMapFrame(cp: Constant_Pool, in: DataInputStream): StackMapFrame

    //
    // IMPLEMENTATION
    //

    def StackMapTable_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        stack_map_frames:     StackMapFrames,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): StackMapTable_attribute

    /**
     * <pre>
     * StackMapTable_attribute {
     *      u2              attribute_name_index;
     *      u4              attribute_length;
     *      u2              number_of_entries;
     *      stack_map_frame entries[number_of_entries];
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
        /*val attribute_length =*/ in.readInt()
        val number_of_entries = in.readUnsignedShort()
        if (number_of_entries > 0 || reifyEmptyAttributes) {
            val frames = fillRefArray(number_of_entries) { StackMapFrame(cp, in) }
            StackMapTable_attribute(
                cp, attribute_name_index, frames, as_name_index, as_descriptor_index
            )
        } else {
            null
        }
    }

    registerAttributeReader(StackMapTableAttribute.Name → parserFactory())
}
