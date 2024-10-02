/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class LocalVariableTypeTableEntry(
    start_pc:        Int,
    length:          Int,
    name_index:      Constant_Pool_Index,
    signature_index: Constant_Pool_Index,
    index:           Int
) {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val name = cp(name_index).toString(cp)
        val signature = cp(signature_index).asString // TODO "Decipher the signature"
        <div class="local_variable">
            <span class="pc">pc=[{start_pc} &rarr; {start_pc + length})</span>
            /
            <span class="local_variable_index"> lv={index}</span>
            &rArr;
            <span class="local_variable_name"> {name}</span>
            :
            <span class="signature"> {signature}</span>
        </div>
    }
}
