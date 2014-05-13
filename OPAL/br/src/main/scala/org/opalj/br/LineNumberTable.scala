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

/**
 * A method's line number table.
 *
 * @author Michael Eichberg
 */
case class LineNumberTable(
    lineNumbers: LineNumbers)
        extends Attribute {

    /**
     * Looks up the line number of the instruction with the given pc.
     *
     * @param pc The program counter/the index of an instruction in the code array for
     *    which we want to determine the source line.
     * @return The line number of the instruction with the given pc, if the line number
     *    is available.
     */
    def lookupLineNumber(pc: PC): Option[Int] = {
        import scala.util.control.Breaks
        val breaks = new Breaks
        import breaks.{ break, breakable }

        val lnsIterator = lineNumbers.iterator
        var lastLineNumber: LineNumber = null
        breakable {
            while (lnsIterator.hasNext) {
                var currentLineNumber = lnsIterator.next
                if (currentLineNumber.startPC <= pc) {
                    lastLineNumber = currentLineNumber
                } else {
                    break
                }
            }
        }

        if (lastLineNumber eq null)
            return None
        else
            return Some(lastLineNumber.lineNumber)
    }

    override def kindId: Int = LineNumberTable.KindId

}
object LineNumberTable {

    final val KindId = 19

}

/**
 * An entry in a line number table.
 *
 * @author Michael Eichberg
 */
case class LineNumber(
    startPC: Int,
    lineNumber: Int)