/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Code_attributeReader

import scala.reflect.ClassTag

/**
 * Binding for the code attribute.
 *
 * @author Michael Eichberg
 */
trait CodeAttributeBinding
    extends Code_attributeReader
    with ConstantPoolBinding
    with CodeBinding
    with AttributeBinding {

    type ExceptionTableEntry = br.ExceptionHandler
    override implicit val exceptionTableEntryType: ClassTag[ExceptionTableEntry] = ClassTag(classOf[br.ExceptionHandler])

    type Code_attribute = br.Code

    def Code_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        max_stack:            Int,
        max_locals:           Int,
        instructions:         Instructions,
        exception_handlers:   ExceptionHandlers,
        attributes:           Attributes
    ): Code_attribute = {
        br.Code(max_stack, max_locals, instructions, exception_handlers, attributes)
    }

    def ExceptionTableEntry(
        cp:               Constant_Pool,
        start_pc:         Int,
        end_pc:           Int,
        handler_pc:       Int,
        catch_type_index: Constant_Pool_Index
    ): ExceptionTableEntry = {
        new ExceptionTableEntry(
            start_pc, end_pc, handler_pc,
            if (catch_type_index == 0)
                None
            else
                Some(cp(catch_type_index).asObjectType(cp))
        )
    }
}
