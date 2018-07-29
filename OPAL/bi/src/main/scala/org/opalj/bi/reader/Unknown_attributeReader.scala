/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * A generic reader that can read attributes that are neither defined by the
 * specification nor by some additional user supplied code.
 */
trait Unknown_attributeReader extends Constant_PoolAbstractions with Unknown_attributeAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    def Unknown_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        info:                 Array[Byte]
    ): Unknown_attribute

    //
    // IMPLEMENTATION
    //

    def Unknown_attribute(
        ap:                   AttributeParent,
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): Unknown_attribute = {
        val info = new Array[Byte](in.readInt)
        in.readFully(info)

        Unknown_attribute(cp, attribute_name_index, info)
    }
}
