/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillAnyRefArray
import org.opalj.collection.immutable.AnyRefArray

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
    type StackMapFrames = AnyRefArray[StackMapFrame]

    def StackMapFrame(cp: Constant_Pool, in: DataInputStream): StackMapFrame

    //
    // IMPLEMENTATION
    //

    def StackMapTable_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        stack_map_frames:     StackMapFrames
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
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt()
        val number_of_entries = in.readUnsignedShort()
        if (number_of_entries > 0 || reifyEmptyAttributes) {
            val frames = fillAnyRefArray(number_of_entries) { StackMapFrame(cp, in) }
            StackMapTable_attribute(cp, attribute_name_index, frames)
        } else {
            null
        }
    }

    registerAttributeReader(StackMapTableAttribute.Name → parserFactory())
}
