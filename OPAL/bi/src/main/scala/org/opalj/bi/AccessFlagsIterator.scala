/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Enables iterating over a class( file member)'s access flags. I.e., given
 * the access flags of a class file, a field or a method, it is then possible
 * to iterate over the flags (synthetic, public, deprecated, etc.) that are set.
 */
class AccessFlagsIterator private (
        private[this] var flags:  Int,
        val potentialAccessFlags: IndexedSeq[AccessFlag]
) extends Iterator[AccessFlag] {

    private[this] var index = -1

    override def hasNext: Boolean = flags != 0

    override def next(): AccessFlag = {
        while ((index + 1) < potentialAccessFlags.size) {
            index += 1
            if ((flags & potentialAccessFlags(index).mask) != 0) {
                flags = flags & (~potentialAccessFlags(index).mask)
                return potentialAccessFlags(index);
            }
        }
        throw BytecodeProcessingFailedException("Unknown access flag(s): 0x"+flags.toHexString)
    }
}

/**
 * Factory for creating `AccessFlagsIterator` objects.
 */
object AccessFlagsIterator {

    /**
     * Creates a new `AccessFlagsIterator`.
     *
     * @param accessFlags The class( file member)'s access flags.
     * @param ctx The accessFlags' context; the interpretation of the access
     *      flags bit vector partially depends on the concrete source element that
     *      defines the accessFlags.
     */
    def apply(accessFlags: Int, ctx: AccessFlagsContext): AccessFlagsIterator = {
        new AccessFlagsIterator(accessFlags, AccessFlagsContexts.potentialAccessFlags(ctx))
    }
}
