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
package org.opalj
package br
package instructions

/**
 * Duplicate the top one or two operand stack values and insert two,
 * three, or four values down.
 *
 * @author Michael Eichberg
 */
case object DUP2_X2 extends StackManagementInstruction {

    final val opcode = 94

    final val mnemonic = "dup2_x2"

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int =
        if (ctg(0) == Category2ComputationalTypeCategory) {
            if (ctg(1) == Category2ComputationalTypeCategory)
                2 // Form 4
            else
                3 // Form 2

        } else {
            if (ctg(2) == Category2ComputationalTypeCategory)
                3 // Form 3
            else
                4 // Form 1
        }

    final def numberOfPushedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int =
        if (ctg(0) == Category2ComputationalTypeCategory) {
            if (ctg(1) == Category2ComputationalTypeCategory)
                3 // Form 4
            else
                4 // Form 2
        } else {
            if (ctg(2) == Category2ComputationalTypeCategory)
                5 // Form 3
            else
                6 // Form 1
        }

}
