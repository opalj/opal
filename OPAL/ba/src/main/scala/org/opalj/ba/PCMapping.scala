/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ba

import java.util.Arrays.copyOf
import java.util.Arrays.fill

/**
 * Mapping of some pc to some new pc. If no mapping exists, Int.MaxValue == PCMapping.Invalid
 * is returned.
 */
class PCMapping(private[ba] var data: Array[Int]) extends (Int => Int) {

    def this(initialSize: Int) =
        this({
            val a = new Array[Int](Math.max(initialSize, 0))
            fill(a, Int.MaxValue)
            a
        })

    def apply(key: Int): Int = {
        if (key >= data.length)
            PCMapping.Invalid
        else
            data(key)
    }

    def +=(key: Int, value: Int): Unit = {
        val oldLength = data.length
        if (key >= oldLength) {
            val newLength = key + 32
            data = copyOf(data, newLength)
            fill(data, oldLength, newLength, Int.MaxValue)
        }
        data(key) = value
    }

}

object PCMapping {
    final def Invalid = Int.MaxValue
}
