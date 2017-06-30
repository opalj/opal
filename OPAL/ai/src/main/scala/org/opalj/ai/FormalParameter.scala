/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj.ai

import org.opalj.br.Method

/**
  * A formal parameter in the TAC representation of a method. For non static methods,
  * the 'this' local is also represented as a formal parameter with origin -1.
  *
  * @param method The method which contains the formal parameter.
  * @param origin The value origin representing the formal parameter.
  *               This value corresponds to the def/use information provided by the
  *               [[org.opalj.tac.TACAI]] transformation. I.e., a UVar with a defSite containing -1,
  *               corresponds to the formal parameter object with origin -1.
  * @note 'this' locals have always an origin of -1.
  *       To get the origin of a parameter with index i use [[org.opalj.ai.parameterIndexToValueOrigin()]].
  * @author Florian Kuebler
  */
final class FormalParameter(final val method: Method, final val origin: ValueOrigin) {

    override def equals(other: Any): Boolean = {
        other match {
            case that: FormalParameter ⇒ (this.method eq that.method) && this.origin == that.origin
            case _ ⇒ false
        }
    }

    override def hashCode(): Int = method.hashCode() * 111 + origin

    override def toString: String = {
        s"FormalParameter(${method.toJava(withVisibility = false)},origin=$origin)"
    }

}

object FormalParameter {

    def apply(method: Method, origin: ValueOrigin): FormalParameter = new FormalParameter(method, origin)

    def unapply(fp: FormalParameter): Some[(Method, ValueOrigin)] = Some((fp.method, fp.origin))

}

