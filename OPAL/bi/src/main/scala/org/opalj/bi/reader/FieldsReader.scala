/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

trait FieldsReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Field_Info <: AnyRef
    implicit val fieldInfoType: ClassTag[Field_Info] // TODO: Replace in Scala 3 by `type Field_Info : ClassTag`
    type Fields = ArraySeq[Field_Info]

    type Attributes

    protected def Attributes(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Attributes

    def Field_Info(
        cp:               Constant_Pool,
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): Field_Info

    //
    // IMPLEMENTATION
    //

    // We need the constant pool to look up the attributes' names and other information.
    def Fields(cp: Constant_Pool, in: DataInputStream): Fields = {
        val fields_count = in.readUnsignedShort
        fillArraySeq(fields_count) {
            Field_Info(cp, in)
        }
    }

    private def Field_Info(cp: Constant_Pool, in: DataInputStream): Field_Info = {
        val accessFlags = in.readUnsignedShort
        val name_index = in.readUnsignedShort
        val descriptor_index = in.readUnsignedShort
        Field_Info(
            cp,
            accessFlags,
            name_index,
            descriptor_index,
            Attributes(cp, AttributesParent.Field, name_index, descriptor_index, in)
        )
    }

}
