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
trait AccessFlagsMatcher { outer ⇒

    def unapply(accessFlags: Int): Boolean

    /**
     * Creates a new matcher that matches accessFlag vectors where all flags
     * defined by this `mask` and the mask of the given matcher are matched.
     */
    def &(accessFlagsMatcher: AccessFlagsMatcher): AccessFlagsMatcher =
        new AccessFlagsMatcher {
            def unapply(accessFlags: Int): Boolean =
                outer.unapply(accessFlags) && accessFlagsMatcher.unapply(accessFlags)
        }

    /**
     * Creates a new matcher that matches accessFlag vectors where none of the flags
     * defined by `mask` is set.
     */
    def unary_!(): AccessFlagsMatcher =
        new AccessFlagsMatcher {
            override def unapply(accessFlags: Int): Boolean = !outer.unapply(accessFlags)
        }
}


