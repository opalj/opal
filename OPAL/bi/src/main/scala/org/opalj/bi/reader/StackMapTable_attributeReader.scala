/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

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
    implicit val stackMapFrameType: ClassTag[StackMapFrame] // TODO: Replace in Scala 3 by `type StackMapFrame : ClassTag`
    type StackMapFrames = ArraySeq[StackMapFrame]

    def StackMapFrame(cp: Constant_Pool, in: DataInputStream): StackMapFrame

    //
    // IMPLEMENTATION
    //

    def StackMapTable_attribute(
        constant_pool:        Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
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
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt()
        val number_of_entries = in.readUnsignedShort()
        if (number_of_entries > 0 || reifyEmptyAttributes) {
            val frames = fillArraySeq(number_of_entries) { StackMapFrame(cp, in) }
            StackMapTable_attribute(
                cp, ap_name_index, ap_descriptor_index, attribute_name_index, frames
            )
        } else {
            null
        }
    }

    registerAttributeReader(StackMapTableAttribute.Name -> parserFactory())
}
