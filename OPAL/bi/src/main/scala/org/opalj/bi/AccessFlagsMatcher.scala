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
package org.opalj
package bi

/**
 * Matches a given access flags bit array and enables the construction of
 * complex matchers.
 *
 * @author Michael Eichberg
 */
sealed trait AccessFlagsMatcher { left ⇒

    def unapply(accessFlags: Int): Boolean

    /**
     * Creates a new matcher that matches `accessFlags` vectors where all flags
     * defined by this matcher and the given matcher have to be defined.
     */
    def &&(right: AccessFlagsMatcher): AccessFlagsMatcher = {
        new AccessFlagsMatcher {

            override def unapply(accessFlags: Int): Boolean =
                left.unapply(accessFlags) && right.unapply(accessFlags)

            override def toString: String = "("+left.toString+" && "+right.toString+")"
        }
    }

    def ||(right: AccessFlagsMatcher): AccessFlagsMatcher = {
        new AccessFlagsMatcher {

            override def unapply(accessFlags: Int): Boolean =
                left.unapply(accessFlags) || right.unapply(accessFlags)

            override def toString: String = "("+left.toString+" || "+right.toString+")"
        }
    }

    /**
     * Creates a new matcher that matches `accessFlags` that do not have (all of) the
     * accessFlags specified by the given matcher.
     */
    def unary_!(): AccessFlagsMatcher =
        new AccessFlagsMatcher {

            override def unapply(accessFlags: Int): Boolean = !left.unapply(accessFlags)

            override def toString: String = "!("+left.toString+")"
        }
}

trait PrimitiveAccessFlagsMatcher extends AccessFlagsMatcher { left ⇒

    /**
     * An integer value that represents an access flags bit mask.
     */
    protected def mask: Int

    override def &&(right: AccessFlagsMatcher): AccessFlagsMatcher = {
        right match {
            case PrimitiveAccessFlagsMatcher(rightMask) ⇒
                new PrimitiveAccessFlagsMatcher {
                    protected val mask = left.mask | rightMask
                    def unapply(accessFlags: Int): Boolean = (accessFlags & mask) == mask
                    override def toString: String = mask.toString
                }
            case _ ⇒ super.&&(right)
        }
    }

    override def unary_!(): AccessFlagsMatcher =
        new AccessFlagsMatcher { // <= it is no longer a primitive matcher
            val mask = left.mask
            override def unapply(accessFlags: Int): Boolean = (accessFlags & mask) != mask
            override def toString: String = "!("+mask.toString+")"
        }
}
object PrimitiveAccessFlagsMatcher {
    def unapply(accessFlagsMatcher: PrimitiveAccessFlagsMatcher): Option[Int] =
        Some(accessFlagsMatcher.mask)
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

    val PUBLIC_INTERFACE = ACC_PUBLIC && ACC_INTERFACE
    val PUBLIC_ABSTRACT = ACC_PUBLIC && ACC_ABSTRACT
    val PUBLIC_FINAL = ACC_PUBLIC && ACC_FINAL
    val PRIVATE_FINAL = ACC_PRIVATE && ACC_FINAL
    val PUBLIC_STATIC = ACC_PUBLIC && ACC_STATIC

    val NOT_INTERFACE = !ACC_INTERFACE
    val NOT_STATIC = !ACC_STATIC
    val NOT_PRIVATE = !ACC_PRIVATE
    val NOT_FINAL = !ACC_FINAL
    val NOT_SYNCHRONIZED = !ACC_SYNCHRONIZED
    val NOT_NATIVE = !ACC_NATIVE
    val NOT_ABSTRACT = !ACC_ABSTRACT

    val PUBLIC_STATIC_FINAL = PUBLIC_FINAL && ACC_STATIC

    val PUBLIC___OR___PROTECTED_AND_NOT_FINAL = ACC_PUBLIC || (ACC_PROTECTED && NOT_FINAL)
}

