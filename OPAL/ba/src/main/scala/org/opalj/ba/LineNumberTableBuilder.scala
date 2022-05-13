/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.collection.immutable.ArraySeq

/**
 * Used to incrementally build the [[org.opalj.br.UnpackedLineNumberTable]].
 *
 * @author Malte Limmeroth
 */
class LineNumberTableBuilder {

    private[this] var lineNumbers: br.LineNumbers = ArraySeq.empty

    def add(element: LINENUMBER, pc: br.PC): this.type = {
        lineNumbers :+= br.LineNumber(pc, element.lineNumber)
        this
    }

    def result(): ArraySeq[br.UnpackedLineNumberTable] = {
        if (lineNumbers.nonEmpty) {
            ArraySeq(br.UnpackedLineNumberTable(lineNumbers))
        } else {
            ArraySeq.empty
        }
    }
}
