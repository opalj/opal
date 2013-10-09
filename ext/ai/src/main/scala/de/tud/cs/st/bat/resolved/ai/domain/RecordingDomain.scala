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
 * A domain that records the values returned by the method/the exceptions thrown
 * by a method.
 *
 * ==Thread Safety==
 * This class is not thread safe. I.e., this domain can only be used if
 * an instance of this domain is not used by multiple threads.
 *
 * @author Michael Eichberg
 */
trait RecordReturnValues[+I] extends Domain[I] {

    var returnedValues: Set[(String, Int, Value)] = Set.empty

    override def areturn(pc: Int, value: DomainValue) {
        returnedValues += (("areturn", pc, value))
    }

    override def dreturn(pc: Int, value: DomainValue) {
        returnedValues += (("dreturn", pc, value))
    }

    override def freturn(pc: Int, value: DomainValue) {
        returnedValues += (("freturn", pc, value))
    }

    override def ireturn(pc: Int, value: DomainValue) {
        returnedValues += (("ireturn", pc, value))
    }

    override def lreturn(pc: Int, value: DomainValue) {
        returnedValues += (("lreturn", pc, value))
    }

    override def returnVoid(pc: Int) {
        returnedValues += (("return", pc, null))
    }

    override def abruptMethodExecution(pc: Int, exception: DomainValue) {
        returnedValues += (("throws", pc, exception))
    }

}

class RecordingDomain[I](identifier: I)
        extends ConfigurableDefaultDomain[I](identifier)
        with RecordReturnValues[I] {

}

