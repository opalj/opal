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
package de.tud.cs.st
package bat
package resolved
package ai
package domain

import language.implicitConversions

/**
 * Records the exceptions thrown by a method. This trait can be used to record
 * the thrown exceptions independently of the precision of the domain.
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
trait DefaultRecordThrownExceptions extends RecordThrownExceptions {

    type ThrownException = ExceptionValue

    override protected[this] def thrownException(pc: PC, value: ExceptionValue): ThrownException =
        value

    override protected[this] def joinThrownExceptions(
        pc: PC,
        previouslyThrownException: ThrownException,
        thrownException: ExceptionValue): ThrownException = {

        previouslyThrownException.join(pc, thrownException) match {
            case NoUpdate                              ⇒ previouslyThrownException
            case StructuralUpdate(exceptionValue)      ⇒ exceptionValue
            case MetaInformationUpdate(exceptionValue) ⇒ exceptionValue
        }
    }
}



