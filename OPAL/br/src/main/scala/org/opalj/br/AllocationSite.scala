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

sealed abstract class AllocationType {
    def name: String
}
case object ObjectAllocation extends AllocationType {
    def name: String = "Object"
}
case object ArrayAllocation extends AllocationType {
    def name: String = "Array"
}

/**
 * An allocation site (a (multi(a))new(array) instruction) in a method's bytecode.
 * (Please see [[org.opalj.br.analyses.AllocationSitesKey]] for further details regarding
 * the default set of allocation sites.)
 *
 * @note   Information about a program's allocation sites can be made available using the
 *         respective key.
 *
 * @author Michael Eichberg
 */
sealed abstract class AllocationSite {

    /**
     * The method which contains this allocation site.
     */
    val method: Method

    /**
     * The unique program counter of the allocation site. I.e., the `(multi(a))new(array)`
     * instruction in the original bytecode as returned by the configured
     * [[org.opalj.bi.reader.ClassFileReader]]. We use the `pc` to ensure that code
     * optimizations/transformations (e.g., transforamtion to three-address code)
     * do not affect the information about the allocation site as such.
     * However, if an allocation site is defined in dead code, it may happen that the
     * transformed/optimized code no longer contains it.
     */
    val pc: PC

    /**
     * The type of the allocation: "Object" or "Array".
     */
    def kind: AllocationType

    def allocatedType: ReferenceType

    final override def equals(other: Any): Boolean = {
        other match {
            case that: AllocationSite ⇒
                // We don't need to the check the type: a given pc in a very specific
                // method never changes it's type.
                (this.method eq that.method) && this.pc == that.pc
            case _ ⇒
                false
        }
    }

    final override def hashCode(): Int = method.hashCode() * 111 + pc

    final override def toString: String = s"${kind.name}AllocationSite(${method.toJava("pc="+pc)})"

}

object AllocationSite {

    def unapply(as: AllocationSite): Some[(Method, PC, AllocationType)] = {
        Some((as.method, as.pc, as.kind))
    }

}

final class ObjectAllocationSite(val method: Method, val pc: PC) extends AllocationSite {
    final def kind: AllocationType = ObjectAllocation

    def allocatedType: ObjectType = method.body.get.instructions(pc).asNEW.objectType
}

object ObjectAllocationSite {

    def apply(method: Method, pc: PC): ObjectAllocationSite = new ObjectAllocationSite(method, pc)

    def unapply(as: ObjectAllocationSite): Some[(Method, PC)] = {
        Some((as.method, as.pc))
    }

}

final class ArrayAllocationSite(val method: Method, val pc: PC) extends AllocationSite {

    final def kind: AllocationType = ArrayAllocation

    def allocatedType: ArrayType = {
        method.body.get.instructions(pc).asCreateNewArrayInstruction.arrayType
    }
}

object ArrayAllocationSite {

    def apply(method: Method, pc: PC): ArrayAllocationSite = new ArrayAllocationSite(method, pc)

    def unapply(as: ArrayAllocationSite): Some[(Method, PC)] = {
        Some((as.method, as.pc))
    }

}
