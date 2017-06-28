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
 * An allocation site (a new instruction) in the raw bytecode of a method. I.e., the bytecode
 * as returned after loading it; load-time transformations, such as the rewriting of
 * invokedynamic instructions, happen before the identification of the allocation sites and
 * are therefore the foundation.
 *
 * @param method The method which contains this allocation site.
 * @param pc     The unique program counter of the allocation site. I.e., the "new" instruction
 *               in the original bytecode as returned by the configured
 *               [[org.opalj.bi.reader.ClassFileReader]]. We use the pc to ensure that code
 *               optimizations/transformations (e.g., transforamtion to three-address code)
 *               do not affect the information about the allocation site as such.
 *               However, if an allocation site is defined in dead code, it may happen that the
 *               transformed/optimized code no longer contains it.
 *
 * @note   Information about a program's allocation sites can be made available.
 *
 * @author Michael Eichberg
 */
final class AllocationSite( final val method: Method, final val pc: PC) {

    override def equals(other: Any): Boolean = {
        other match {
            case that: AllocationSite ⇒ (this.method eq that.method) && this.pc == that.pc
            case _                    ⇒ false
        }
    }

    override def hashCode(): Int = method.hashCode() * 111 + pc

    override def toString: String = {
        s"AllocationSite(${method.toJava(withVisibility = false)},pc=$pc)"
    }

}

object AllocationSite {

    def apply(method: Method, pc: PC): AllocationSite = new AllocationSite(method, pc)

    def unapply(as: AllocationSite): Some[(Method, PC)] = Some((as.method, as.pc))

}
