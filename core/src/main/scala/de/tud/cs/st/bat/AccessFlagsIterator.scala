/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * @param accessFlags the class( file member)'s access flags
 * @param ctx the accessFlags' context; the interpretation of the access
 * 	flags bit vector partially depends on the concrete source element that
 * 	defines the accessFlags.
 *
 * @author Michael Eichberg
 */
final class AccessFlagsIterator(
    accessFlags: Int,
    ctx: AccessFlagsContext)
        extends Iterator[AccessFlag] {

    private[this] var flags = accessFlags

    private[this] val potentialAccessFlags = AccessFlagsContexts.potentialAccessFlags(ctx)

    private[this] var index = -1

    def hasNext = flags != 0

    def next: AccessFlag = {
        while ((index + 1) < potentialAccessFlags.size) {
            index += 1
            if ((flags & potentialAccessFlags(index).mask) != 0) {
                flags = flags & (~potentialAccessFlags(index).mask)
                return potentialAccessFlags(index)
            }
        }
        BATError("Unknown access flag(s): "+Integer.toHexString(flags))
    }
}
object AccessFlagsIterator {

    def apply(accessFlags: Int, ctx: AccessFlagsContext) =
        new AccessFlagsIterator(accessFlags, ctx)

}

