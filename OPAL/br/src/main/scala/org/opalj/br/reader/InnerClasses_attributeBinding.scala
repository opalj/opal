/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.InnerClasses_attributeReader

import scala.reflect.ClassTag

/**
 * The factory methods to create inner classes attributes and entries.
 *
 * @author Michael Eichberg
 */
trait InnerClasses_attributeBinding
    extends InnerClasses_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type InnerClasses_attribute = br.InnerClassTable
    type InnerClassesEntry = br.InnerClass
    override implicit val innerClassesEntryType: ClassTag[InnerClassesEntry] = ClassTag(classOf[br.InnerClass])

    def InnerClasses_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        inner_classes:        InnerClasses
    ): InnerClasses_attribute =
        new InnerClasses_attribute(inner_classes)

    def InnerClassesEntry(
        cp:                       Constant_Pool,
        inner_class_info_index:   Constant_Pool_Index,
        outer_class_info_index:   Constant_Pool_Index,
        inner_name_index:         Constant_Pool_Index,
        inner_class_access_flags: Int
    ): InnerClassesEntry = {
        new InnerClassesEntry(
            cp(inner_class_info_index).asObjectType(cp),
            if (outer_class_info_index == 0)
                None
            else
                Some(cp(outer_class_info_index).asObjectType(cp)),
            if (inner_name_index == 0)
                None
            else
                Some(cp(inner_name_index).asString),
            inner_class_access_flags
        )
    }
}

