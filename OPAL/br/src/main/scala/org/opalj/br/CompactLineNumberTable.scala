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
case class CompactLineNumberTable(
    lineNumbers: Array[Byte])
        extends LineNumberTable {

    def lookupLineNumber(pc: PC): Option[Int] = {
        /* FORMAT
         *  <pre>
         * LineNumberTable_attribute {
         *   DATA:
         *   {  u2 start_pc;
         *      u2 line_number;
         *   }  
         * }
         * </pre>
         *
         */
        val breaks = new scala.util.control.Breaks
        import breaks.{ break, breakable }

        var lastLineNumber: Option[Int] = None
        breakable {
            var e = 0
            val entries = lineNumbers.size / 4
            println("ENTRIES:"+entries+"  ....  "+lineNumbers.grouped(4).map(_.mkString(",")).mkString(" - "))
            while (e < entries) {
                val index = e * 4
                val startPC = ((lineNumbers(index) & 0xFF) << 8) + (lineNumbers(index + 1) & 0xFF)
                if (startPC <= pc) {
                    val currentLineNumber =
                        ((lineNumbers(index + 2) & 0xFF) << 8) + (lineNumbers(index + 3) & 0xFF)
                    println(pc+": start_pc "+startPC+" => "+currentLineNumber)
                    lastLineNumber = Some(currentLineNumber)
                } else {
                    break
                }
                e += 1
            }
        }
        println("FOUND:"+lastLineNumber+"\n\n")
        lastLineNumber
    }

}
