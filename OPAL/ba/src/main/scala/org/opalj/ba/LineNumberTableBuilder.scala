/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.collection.immutable.RefArray

/**
 * Used to incrementally build the [[org.opalj.br.UnpackedLineNumberTable]].
 *
 * @author Malte Limmeroth
 */
class LineNumberTableBuilder {

    private[this] var lineNumbers: br.LineNumbers = RefArray.empty

    def add(element: LINENUMBER, pc: br.PC): this.type = {
        lineNumbers :+= br.LineNumber(pc, element.lineNumber)
        this
    }

    def result(): RefArray[br.UnpackedLineNumberTable] = {
        if (lineNumbers.nonEmpty) {
            RefArray(br.UnpackedLineNumberTable(lineNumbers))
        } else {
            RefArray.empty
        }
    }
}
