/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for `RuntimeVisibleAnnotations` attribute.
 */
trait RuntimeVisibleAnnotations_attributeReader extends AttributeReader {

    type Annotation

    type Annotations <: Traversable[Annotation]
    def Annotations(cp: Constant_Pool, in: DataInputStream): Annotations

    type RuntimeVisibleAnnotations_attribute >: Null <: Attribute

    def RuntimeVisibleAnnotations_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
    ): RuntimeVisibleAnnotations_attribute

    //
    // IMPLEMENTATION
    //

    private[this] def parserFactory() = (
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt()
        val annotations = Annotations(cp, in)
        if (annotations.nonEmpty || reifyEmptyAttributes) {
            RuntimeVisibleAnnotations_attribute(cp, attribute_name_index, annotations)
        } else {
            null
        }
    }

    registerAttributeReader(RuntimeVisibleAnnotationsAttribute.Name → parserFactory())
}
