/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.br.cfg

import org.opalj.br.Code
import org.opalj.br.PC
import org.opalj.br.ExceptionHandler

/**
 * @author Erich Wittenbeck
 */
class CatchBlock(val handler: ExceptionHandler) extends CFGBlock {

    final def startPC: PC = handler.startPC
    final def endPC: PC = handler.endPC
    final def handlerPC: PC = handler.handlerPC

    override def equals(any: Any): Boolean = {
        any match {
            case that: CatchBlock ⇒ this.handler == that.handler // TODO This is questionable (how about the id field!)
            case _                ⇒ false
        }
    }

    override def hashCode(): Int = 973235 * 51 + handler.hashCode; // TODO This is questionable (how about the id field!)

    def toDot(code: Code): String = {
        var res: String = ID+" [shape=box, label=\""+ID+"\"];\n"

        for (succ ← successors) {
            res = res + ID+" -> "+succ.ID+";\n"
        }

        res
    }
}
