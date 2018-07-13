/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Deprecated_attribute(attribute_name_index: Constant_Pool_Index) extends Attribute {

    final override def attribute_length = 0

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="details deprecated_attribute">Deprecated</div>
    }
}
