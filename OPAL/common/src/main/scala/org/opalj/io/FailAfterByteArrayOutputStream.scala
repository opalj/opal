/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package io

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * A `java.io.ByteArrayOutputStream` that throws an `IOException` after writing
 * some bytes, if the given boundary (`failAfter`) is surpassed.
 *
 * @param  failAfter Sets the boundary after which a `write` will throw an
 *         exception.
 * @param  initialSize Sets the initial size of the array used to the store the content.
 *         This serves optimization purposes only.
 * @author Michael Eichberg
 */
class FailAfterByteArrayOutputStream(
        failAfter: Int
)(
        initialSize: Int = Math.min(32, failAfter)
) extends ByteArrayOutputStream(initialSize) {

    /**
     * Writes the given byte value to the underlying array and then checks if the
     * given boundary (`failAfter`) was (already) passed.
     *
     * I.e., the underlying array contains the given values.
     */
    override def write(b: Int): Unit = this.synchronized {
        super.write(b)
        if (size >= failAfter) {
            throw new IOException(s"more than $failAfter bytes have been written ($size)")
        }
    }

    /**
     * Writes the given byte value to the underlying array and then checks if the
     * given boundary `failAfter` was (already) passed.
     *
     * I.e., the underlying array contains the given values.
     */
    override def write(b: Array[Byte], off: Int, len: Int): Unit = this.synchronized {
        super.write(b, off, len)
        if (size >= failAfter) {
            throw new IOException(s"more than $failAfter bytes have been written ($size)")
        }

    }
}
