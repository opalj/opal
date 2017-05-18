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
package ba

/**
 * Pseudo instruction that generates an entry in the [[org.opalj.br.LineNumberTable]] with the
 * program counter of the following instruction.
 *
 * @author Malte Limmeroth
 */
case class LINENUMBER(lineNumber: Int) extends PseudoInstruction

/**
 * Incrementally builds a [[org.opalj.br.UnpackedLineNumberTable]].
 *
 * @author Malte Limmeroth
 */
class LineNumberTableGenerator {

    private var lineNumbers: br.LineNumbers = Seq.empty

    def add(element: LINENUMBER, pc: br.PC) = {
        lineNumbers :+= br.LineNumber(pc, element.lineNumber)
    }

    def finalizeLineNumberTable: Option[br.UnpackedLineNumberTable] = {
        if (lineNumbers.size > 0) {
            Some(br.UnpackedLineNumberTable(lineNumbers))
        } else {
            None
        }

    }
}
