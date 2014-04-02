/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st
package bat

/**
 * Enables iterating over a class( file member)'s access flags. I.e., given
 * the access flags of a class file, a field or a method, it is then possible
 * to iterate over the flags (synthetic, public, deprecated, etc.) that are set.
 *
 * @author Michael Eichberg
 */
class AccessFlagsIterator private (
    private[this] var flags: Int,
    val potentialAccessFlags: IndexedSeq[AccessFlag])
        extends Iterator[AccessFlag] {

    private[this] var index = -1

    override def hasNext = flags != 0

    override def next: AccessFlag = {
        while ((index + 1) < potentialAccessFlags.size) {
            index += 1
            if ((flags & potentialAccessFlags(index).mask) != 0) {
                flags = flags & (~potentialAccessFlags(index).mask)
                return potentialAccessFlags(index)
            }
        }
        throw new BATException("Unknown access flag(s): "+Integer.toHexString(flags))
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
    def apply(accessFlags: Int, ctx: AccessFlagsContext) =
        new AccessFlagsIterator(accessFlags, AccessFlagsContexts.potentialAccessFlags(ctx))
}

