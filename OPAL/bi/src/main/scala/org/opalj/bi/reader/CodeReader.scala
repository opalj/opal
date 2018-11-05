/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Naive Code reader that just reads in the code array as is - without parsing it.
 */
trait CodeReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Instructions

    def Instructions(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        instructions:        Array[Byte]
    ): Instructions

    //
    // IMPLEMENTATION
    //

    def Instructions(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Instructions = {
        val code_length = in.readInt
        val the_code = new Array[Byte](code_length)
        in.readFully(the_code)

        Instructions(cp, ap_name_index, ap_descriptor_index, the_code)
    }
}
