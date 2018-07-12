/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 *
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Synthetic_attribute(attribute_name_index: Constant_Pool_Index) extends Attribute {

    final override def attribute_length = 0

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="details attribute">Synthetic</div>
    }

}
