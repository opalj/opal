/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.Text

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class ElementValuePair(
        element_name_index: Constant_Pool_Index,
        element_value:      ElementValue
) {

    final def attribute_length: Int = 2 + element_value.attribute_length

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val name = cp(element_name_index).toString(cp)

        <li class="element_value_pair">
            { Seq(<span class="element_name">{ name }</span>, Text("="), element_value.toXHTML) }
        </li>
    }

}
