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
package ai
package domain
package l0

/**
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait ReturnInstructionsDomain extends ai.ReturnInstructionsDomain with MonitorInstructionsTracker {
    domain: ValuesDomain with Configuration with ExceptionsFactory ⇒

    /**
     * Creates a computation object that encapsulates the result of a computation that
     * may throw an `IllegalMonitorStateException` if a monitor is (potentially) used.
     * The primary example are the `(XXX)return` instructions.
     *
     * @param pc The program counter of a return instruction.
     */
    protected[this] def handleReturn(pc: Int): Computation[Nothing, ExceptionValue] = {
        if (isMonitorInstructionUsed && throwIllegalMonitorStateException) {
            val exception = IllegalMonitorStateException(ValueOriginForVMLevelValue(pc))
            ComputationWithSideEffectOrException(exception)
        } else {
            ComputationWithSideEffectOnly
        }
    }

}
