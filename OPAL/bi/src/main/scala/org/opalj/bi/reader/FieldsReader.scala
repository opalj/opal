/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

trait FieldsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type Field_Info
    implicit val Field_InfoManifest: ClassTag[Field_Info]

    type Attributes

    protected def Attributes(
        ap: AttributeParent,
        cp: Constant_Pool,
        in: DataInputStream
    ): Attributes

    def Field_Info(
        constant_pool:    Constant_Pool,
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): Field_Info

    //
    // IMPLEMENTATION
    //

    type Fields = IndexedSeq[Field_Info]

    // We need the constant pool to look up the attributes' names and other information.
    def Fields(cp: Constant_Pool, in: DataInputStream): Fields = {
        val fields_count = in.readUnsignedShort
        repeat(fields_count) {
            Field_Info(cp, in)
        }
    }

    private def Field_Info(cp: Constant_Pool, in: DataInputStream): Field_Info =
        Field_Info(
            cp,
            in.readUnsignedShort,
            in.readUnsignedShort,
            in.readUnsignedShort,
            Attributes(AttributesParent.Field, cp, in)
        )

}
