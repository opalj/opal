/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.Exceptions_attributeReader

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
    val Exceptions_attributeManifest: ClassTag[Exceptions_attribute] = implicitly

    def Exceptions_attribute(
        cp:                    Constant_Pool,
        attribute_name_index:  Constant_Pool_Index,
        exception_index_table: ExceptionIndexTable
    ): Exceptions_attribute = {
        new Exceptions_attribute(
            exception_index_table.map(e_idx ⇒ cp(e_idx).asObjectType(cp))
        )
    }
}

