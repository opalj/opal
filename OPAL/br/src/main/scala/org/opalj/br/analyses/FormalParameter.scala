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
package org.opalj
package br
package analyses

/**
 * Explicitly models a formal parameter of a method to make it possible to store it in the
 * property store and to compute properties for it. The first parameter explicitly defined by
 * the method will have the origin `-2`, the second one will have the origin `-3` and so on.
 * That is, the origin of an explicitly declared parameter is always `-(p_index + 2)`.
 * In case of an instance method the origin of the this parameter is `-1`.
 *
 * @note The computational type category of the parameters is ignored to ease the mapping.
 *
 * @note This encoding is also used by the default three address code representation
 *       generated using a local data-flow analysis (see [[org.opalj.tac.TACAI]]).
 *
 *       '''In case of the bytecode based data-flow analysis the origin used by the analysis
 *       reflects the position of the parameter value on the tac; see
 *       [[org.opalj.ai.parameterIndexToValueOrigin]].'''
 *
 *
 * @param method The method which contains the formal parameter.
 * @param origin The origin associated with the parameter. See the general description for
 *               further details.
 *
 * @author Florian Kuebler
 */
final class FormalParameter( final val method: Method, final val origin: Int) {

    /**
     * @return The index of the parameter or -1 if this Formal Parameter reflects the
     *         implicit `this` value.
     */
    def parameterIndex = -origin - 2

    override def equals(other: Any): Boolean = {
        other match {
            case that: FormalParameter ⇒ (this.method eq that.method) && this.origin == that.origin
            case _                     ⇒ false
        }
    }

    override def hashCode(): Int = method.hashCode() * 111 + origin

    override def toString: String = {
        s"FormalParameter(${method.toJava(withVisibility = false)},origin=$origin)"
    }

}

object FormalParameter {

    def apply(method: Method, origin: Int): FormalParameter = new FormalParameter(method, origin)

    def unapply(fp: FormalParameter): Some[(Method, Int)] = Some((fp.method, fp.origin))

}
