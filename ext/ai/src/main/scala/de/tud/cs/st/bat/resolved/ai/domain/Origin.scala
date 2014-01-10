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
package resolved
package ai
package domain

/**
 * @author Michael Eichberg
 */
trait Origin { this : SomeDomain => 

    trait SingleOriginValue  {
        def pc: PC
    }

    object SingleOriginValue {
        def unapply(value: SingleOriginValue): Option[PC] = Some(value.pc)
    }

    trait MultipleOriginsValue {
        def pcs: Iterable[PC]
    }

    object MultipleOriginsValue {
        def unapply(value: MultipleOriginsValue): Option[Iterable[PC]] = Some(value.pcs)
    }

    /**
     * Returns the origin(s) of the value if the information is available.
     *
     * @return The source(s) of the given value if the information is available.
     *      Whether the information is available depends on the concrete domains.
     *      This trait only defines a general contract how to get access to a
     *      value's origin (I.e., the pc of the instruction which created the
     *      respective value.)
     *      By default this method returns an empty `Iterable`.
     */
    abstract override def origin(value: DomainValue): Iterable[PC] =
        value match {
            case SingleOriginValue(pc)     ⇒ Iterable[PC](pc)
            case MultipleOriginsValue(pcs) ⇒ pcs
            case _                         ⇒ Iterable.empty
        }

}
