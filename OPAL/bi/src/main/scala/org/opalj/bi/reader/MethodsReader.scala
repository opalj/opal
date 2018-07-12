/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

/**
 * Defines a template method to read in a class file's Method_info structure.
 */
trait MethodsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type Attributes

    protected def Attributes(
        ap: AttributeParent,
        cp: Constant_Pool,
        in: DataInputStream
    ): Attributes

    type Method_Info
    implicit val Method_InfoManifest: ClassTag[Method_Info]

    def Method_Info(
        constant_pool:    Constant_Pool,
        accessFlags:      Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): Method_Info

    //
    // IMPLEMENTATION
    //

    type Methods = IndexedSeq[Method_Info]

    def Methods(cp: Constant_Pool, in: DataInputStream): Methods = {
        val methods_count = in.readUnsignedShort
        repeat(methods_count) {
            Method_Info(cp, in)
        }
    }

    private def Method_Info(cp: Constant_Pool, in: DataInputStream): Method_Info = {
        Method_Info(
            cp,
            in.readUnsignedShort,
            in.readUnsignedShort,
            in.readUnsignedShort,
            Attributes(AttributesParent.Method, cp, in)
        )
    }

}
