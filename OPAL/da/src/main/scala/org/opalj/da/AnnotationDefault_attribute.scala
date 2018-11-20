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
case class AnnotationDefault_attribute(
        attribute_name_index: Constant_Pool_Index,
        element_value:        ElementValue
) extends Attribute {

    final def attribute_length: Int = element_value.attribute_length

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute">
            <summary class="attribute_name">AnnotationDefault</summary>
            { element_value.toXHTML }
        </details>
    }

}
