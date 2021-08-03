/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

/**
 * An entry in a line number table.
 *
 * @author Michael Eichberg
 */
case class LineNumber(startPC: PC, lineNumber: Int) {

    def remapPCs(codeSize: Int, f: PC => PC): Option[LineNumber] = {
        val newStartPC = f(startPC)
        if (newStartPC < codeSize)
            Some(LineNumber(newStartPC, lineNumber))
        else
            None
    }

}
