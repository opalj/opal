/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Record_attributeReader

import scala.reflect.ClassTag

/**
 * Implements the factory methods to create the `Record` attribute (Java 16).
 *
 * @author Dominik Helm
 */
trait Record_attributeBinding
    extends Record_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Record_attribute = br.Record

    type RecordComponent = br.RecordComponent
    override implicit val recordComponentType: ClassTag[RecordComponent] = ClassTag(classOf[br.RecordComponent])

    override def Record_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        components:           RecordComponents
    ): Record_attribute = {
        new Record(components)
    }

    override def RecordComponent(
        cp:               Constant_Pool,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): RecordComponent = {
        val componentName = cp(name_index).asString
        val componentType = cp(descriptor_index).asFieldType
        new RecordComponent(componentName, componentType, attributes)
    }

}
