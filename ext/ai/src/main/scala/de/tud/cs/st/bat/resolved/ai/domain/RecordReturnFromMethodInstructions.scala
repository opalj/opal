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

import language.implicitConversions

import collection.mutable.UShortSet

/**
 * Records the program counters of all instructions that lead to a abnormal/normal
 * return from the method.
 *
 * ==Usage==
 * A domain that mixes in this trait should only be used to analyze a single method.
 *
 * ==Thread Safety==
 * This class is not thread safe. I.e., this domain can only be used if
 * an instance of this domain is not used by multiple threads.
 *
 * @author Michael Eichberg
 */
trait RecordReturnFromMethodInstructions[+I] extends Domain[I] {

    @volatile private[this] var returnInstructions: UShortSet = UShortSet.empty

    def allReturnInstructions: PCs = returnInstructions

    override def areturn(pc: PC, value: DomainValue) {
        returnInstructions += pc
    }

    override def dreturn(pc: PC, value: DomainValue) {
        returnInstructions += pc
    }

    override def freturn(pc: PC, value: DomainValue) {
        returnInstructions += pc
    }

    override def ireturn(pc: PC, value: DomainValue) {
        returnInstructions += pc
    }

    override def lreturn(pc: PC, value: DomainValue) {
        returnInstructions += pc
    }

    override def returnVoid(pc: PC) {
        returnInstructions += pc
    }

    // handles all kinds of abrupt method returns 
    override def abruptMethodExecution(pc: PC, exception: DomainValue) {
        returnInstructions += pc
    }
}



