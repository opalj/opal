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
 * Matches a given access flags bit array and enables the construction of
 * complex matchers.
 *
 * @author Michael Eichberg
 */
trait AccessFlagsMatcher { left ⇒

    def unapply(accessFlags: Int): Boolean

    protected def mask: Int

    /**
     * Creates a new matcher that matches `accessFlags` vectors where all flags
     * defined by this `mask` and the mask of the given matcher are matched.
     */
    def &(right: AccessFlagsMatcher): AccessFlagsMatcher =
        new AccessFlagsMatcher {
            val mask = left.mask | right.mask
            def unapply(accessFlags: Int): Boolean = (accessFlags & mask) == mask
        }

    /**
     * Creates a new matcher that matches `accessFlags` vectors where none of the flags
     * defined by `mask` is set.
     */
    def unary_!(): AccessFlagsMatcher =
        new AccessFlagsMatcher {
            val mask = left.mask
            override def unapply(accessFlags: Int): Boolean = (accessFlags & mask) == 0
        }
}

/**
 * Predefines several access flags matchers.
 *
 * @example
 * The predefined matchers are used in the following way:
 * {{{
 *  method match { case Method(PUBLIC_STATIC(),...) => ... } 
 *  field match { case Field(PUBLIC_STATIC_FINAL(),...) => ... }
 * }}}
 *
 * @author Michael Eichberg
 */
object AccessFlagsMatcher {

    val PUBLIC_INTERFACE = ACC_PUBLIC & ACC_INTERFACE
    val PUBLIC_ABSTRACT = ACC_PUBLIC & ACC_ABSTRACT
    val PUBLIC_FINAL = ACC_PUBLIC & ACC_FINAL
    val PRIVATE_FINAL = ACC_PRIVATE & ACC_FINAL
    val PUBLIC_STATIC = ACC_PUBLIC & ACC_STATIC
    val PUBLIC_STATIC_FINAL = PUBLIC_FINAL & ACC_STATIC
    val NOT_INTERFACE = !ACC_INTERFACE
    val NOT_STATIC = !ACC_STATIC
    val NOT_PRIVATE = !ACC_PRIVATE
    val NOT_FINAL = !ACC_FINAL
    val NOT_SYNCHRONIZED = !ACC_SYNCHRONIZED
    val NOT_NATIVE = !ACC_NATIVE
    val NOT_ABSTRACT = !ACC_ABSTRACT
}


