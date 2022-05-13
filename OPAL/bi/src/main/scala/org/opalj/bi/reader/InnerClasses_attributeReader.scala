/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the ''inner classes'' attribute.
 */
trait InnerClasses_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type InnerClassesEntry <: AnyRef
    implicit val innerClassesEntryType: ClassTag[InnerClassesEntry] // TODO: Replace in Scala 3 by `type InnerClassesEntry : ClassTag`
    type InnerClasses = ArraySeq[InnerClassesEntry]

    type InnerClasses_attribute >: Null <: Attribute

    def InnerClasses_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        inner_classes:        InnerClasses
    ): InnerClasses_attribute

    def InnerClassesEntry(
        cp:                       Constant_Pool,
        inner_class_info_index:   Constant_Pool_Index,
        outer_class_info_index:   Constant_Pool_Index,
        inner_name_index:         Constant_Pool_Index,
        inner_class_access_flags: Int
    ): InnerClassesEntry

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * InnerClasses_attribute {
     * u2 attribute_name_index;
     * u4 attribute_length;
     * u2 number_of_classes; // => Seq[InnerClasses_attribute.Class]
     *  {   u2 inner_class_info_index;
     *      u2 outer_class_info_index;
     *      u2 inner_name_index;
     *      u2 inner_class_access_flags;
     *  } classes[number_of_classes];
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
        val number_of_classes = in.readUnsignedShort
        if (number_of_classes > 0 || reifyEmptyAttributes) {
            InnerClasses_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                fillArraySeq(number_of_classes) {
                    InnerClassesEntry(
                        cp,
                        in.readUnsignedShort, in.readUnsignedShort,
                        in.readUnsignedShort, in.readUnsignedShort
                    )
                }
            )
        } else {
            null
        }
    }

    registerAttributeReader(InnerClassesAttribute.Name -> parserFactory())
}
