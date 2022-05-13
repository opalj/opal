/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Defines a template method to read in a class file's Method_info structure.
 */
trait MethodsReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Method_Info <: AnyRef
    implicit val methodInfoType: ClassTag[Method_Info] // TODO: Replace in Scala 3 by `type Method_Info : ClassTag`
    type Methods = ArraySeq[Method_Info]

    type Attributes

    protected def Attributes(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index, // -1 if no descriptor is available; i.e., the parent is the class file
        in:                  DataInputStream
    ): Attributes

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

    def Methods(cp: Constant_Pool, in: DataInputStream): Methods = {
        val methods_count = in.readUnsignedShort
        fillArraySeq(methods_count) {
            Method_Info(cp, in)
        }
    }

    private def Method_Info(cp: Constant_Pool, in: DataInputStream): Method_Info = {
        val accessFlags = in.readUnsignedShort
        val name_index = in.readUnsignedShort
        val descriptor_index = in.readUnsignedShort
        Method_Info(
            cp,
            accessFlags,
            name_index,
            descriptor_index,
            Attributes(cp, AttributesParent.Method, name_index, descriptor_index, in)
        )
    }
}
