/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.NestMembers_attributeReader

import scala.collection.immutable.ArraySeq

/**
 * The factory method to create the `NestMembers` attribute (Java 11).
 *
 * @author Dominik Helm
 */
trait NestMembers_attributeBinding
    extends NestMembers_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type NestMembers_attribute = NestMembers

    def NestMembers_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        classes_array:        ClassesArray // CONSTANT_Class_info[]
    ): NestMembers_attribute = {
        new NestMembers(
            ArraySeq.from(classes_array).map { p => cp(p).asObjectType(cp) }
        )
    }

}

