/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

/**
 * Used to incrementally build the [[org.opalj.br.UnpackedLineNumberTable]].
 *
 * @author Malte Limmeroth
 */
class LineNumberTableBuilder {

    private[this] var lineNumbers: br.LineNumbers = IndexedSeq.empty

    def add(element: LINENUMBER, pc: br.PC) = {
        lineNumbers :+= br.LineNumber(pc, element.lineNumber)
    }

    def result(): IndexedSeq[br.UnpackedLineNumberTable] = {
        if (lineNumbers.nonEmpty) {
            IndexedSeq(br.UnpackedLineNumberTable(lineNumbers))
        } else {
            IndexedSeq.empty
        }
    }
}
