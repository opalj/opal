/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

/**
 * Naive Code reader that just reads in the code array as is - without parsing it.
 */
trait CodeReader extends Constant_PoolAbstractions {

    type Instructions

    def Instructions(cp: Constant_Pool, instructions: Array[Byte]): Instructions

    def Instructions(cp: Constant_Pool, in: DataInputStream): Instructions = {
        val code_length = in.readInt
        val the_code = new Array[Byte](code_length)
        in.readFully(the_code)

        Instructions(cp, the_code)
    }
}
