/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the ''Record'' attribute (Java 16).
 *
 * '''From the Specification'''
 * The Record attribute is a variable-length attribute in the attributes table of a ClassFile
 * structure (§4.1). The Record attribute indicates that the current class is a record class, and
 * stores information about the record components of the record class
 *
 * @author Dominik Helm
 */
trait Record_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Record_attribute >: Null <: Attribute

    type RecordComponent <: AnyRef
    implicit val recordComponentType: ClassTag[RecordComponent] // TODO: Replace in Scala 3 by `type RecordComponent : ClassTag`
    type RecordComponents = ArraySeq[RecordComponent]

    def Record_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        components:           RecordComponents
    ): Record_attribute

    def RecordComponent(
        cp:               Constant_Pool,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): RecordComponent

    protected def Attributes(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Attributes

    //
    // IMPLEMENTATION
    //

    /**
     * <pre>
     * Record_attribute {
     *      u2 attribute_name_index;
     *      u4 attribute_length;
     *      u2 components_count;
     *      record_component_info[components_count];
     * }
     *
     * record_component_info {
     *      u2 name_index;
     *      u2 descriptor_index;
     *      u2 attributes_count;
     *      attribute_info attributes[attributes_count];
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
        val components_count = in.readUnsignedShort
        if (components_count > 0 || reifyEmptyAttributes) {
            Record_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                fillArraySeq(components_count) {
                    {
                        val name_index = in.readUnsignedShort
                        val descriptor_index = in.readUnsignedShort
                        RecordComponent(
                            cp,
                            name_index,
                            descriptor_index,
                            Attributes(
                                cp,
                                AttributesParent.RecordComponent,
                                name_index,
                                descriptor_index,
                                in
                            )
                        )
                    }
                }
            )
        } else {
            null
        }
    }: Record_attribute

    registerAttributeReader(RecordAttribute.Name -> parserFactory())

}
