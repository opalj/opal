/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Tobias Becker
 */
case class LocalVariableTableEntry(
        start_pc:         Int,
        length:           Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        index:            Int
) {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val name = cp(name_index).toString(cp)
        val descriptor = parseFieldType(cp(descriptor_index).asString)
        <div class="local_variable">
            <span class="pc">pc=[{ start_pc } &rarr; { start_pc + length })</span>
            /
            <span class="index"> lv={ index }</span>
            &rArr;
            { descriptor.asSpan("") }
            &nbsp;
            <span class="name"> { name }</span>
        </div>
    }

}
