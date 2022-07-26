/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Exceptions_attributeReader

import scala.collection.immutable.ArraySeq

/**
 * The factory method to create a method's exception attribute.
 *
 * @author Michael Eichberg
 */
trait Exceptions_attributeBinding
    extends Exceptions_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Exceptions_attribute = br.ExceptionTable

    def Exceptions_attribute(
        cp:                    Constant_Pool,
        ap_name_index:         Constant_Pool_Index,
        ap_descriptor_index:   Constant_Pool_Index,
        attribute_name_index:  Constant_Pool_Index,
        exception_index_table: Array[Constant_Pool_Index]
    ): Exceptions_attribute = {
        new Exceptions_attribute(
            ArraySeq.from(exception_index_table).map(e_index => cp(e_index).asObjectType(cp))
        )
    }
}

