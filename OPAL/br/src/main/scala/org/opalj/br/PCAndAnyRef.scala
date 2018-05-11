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

/**
 * An efficient (i.e., no (un)boxing...) representation of an instruction and a value.
 *
 * @author Michael Eichberg
 */
/* no case class */ final class PCAndAnyRef[T <: AnyRef](val pc: Int /* PC */ , val value: T) {

    override def hashCode(): Opcode = value.hashCode() * 117 + pc

    override def equals(other: Any): Boolean = {
        other match {
            case that: PCAndAnyRef[_] ⇒ this.pc == that.pc && this.value == that.value
            case _                    ⇒ false
        }
    }

    override def toString: String = s"PCAndAnyRef(pc=$pc,$value)"
}

object PCAndAnyRef {
    def apply[T <: AnyRef](pc: Int /* PC */ , value: T): PCAndAnyRef[T] = {
        new PCAndAnyRef(pc, value)
    }

    // TODO Figure out if the (un)boxing related to the matcher is relevant or optimized away by the JVM
    def unapply[T <: AnyRef](pcAndValue: PCAndAnyRef[T]): Some[(Int /* PC */ , T)] = {
        Some((pcAndValue.pc, pcAndValue.value))
    }
}
