/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package instructions

/**
 * Jump subroutine.
 *
 * @author Michael Eichberg
 */
trait JSR_WLike extends JSRInstructionLike {

    final def opcode: Opcode = JSR_W.opcode

    final def mnemonic: String = "jsr_w"

    final def length: Int = 5
}

case class JSR_W(branchoffset: Int) extends JSRInstruction with JSR_WLike

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object JSR_W {

    final val opcode = 201

    /**
     * Creates [[LabeledJSR_W]] instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: Symbol): LabeledJSR_W = LabeledJSR_W(branchTarget)
}

case class LabeledJSR_W(
        branchTarget: Symbol
) extends LabeledUnconditionalBranchInstruction with JSRLike {
    override def resolveJumpTargets(currentIndex: PC, branchoffsets: Map[Symbol, PC]): JSR_W = {
        JSR_W(branchoffsets(branchTarget) - currentIndex)
    }

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        this eq code.instructions(otherPC)
    }
}