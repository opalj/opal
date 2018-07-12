/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A method's line number table.
 *
 * @author Michael Eichberg
 */
case class UnpackedLineNumberTable(lineNumbers: LineNumbers) extends LineNumberTable {

    /**
     * Looks up the line number of the instruction with the given pc.
     *
     * @param  pc The program counter/the index of an instruction in the code array for
     *         which we want to determine the source line.
     * @return The line number of the instruction with the given pc, if the line number
     *         is available.
     */
    def lookupLineNumber(pc: PC): Option[Int] = {
        import scala.util.control.Breaks
        val breaks = new Breaks
        import breaks.{break, breakable}

        val lnsIterator = lineNumbers.iterator
        var lastLineNumber: LineNumber = null
        breakable {
            while (lnsIterator.hasNext) {
                val currentLineNumber = lnsIterator.next()
                if (currentLineNumber.startPC <= pc) {
                    lastLineNumber = currentLineNumber
                } else {
                    break()
                }
            }
        }

        if (lastLineNumber eq null)
            None
        else
            Some(lastLineNumber.lineNumber)
    }

    def firstLineNumber(): Option[Int] = {
        if (lineNumbers.isEmpty)
            None
        else
            Some(lineNumbers.view.map(_.lineNumber).min)
    }

}

