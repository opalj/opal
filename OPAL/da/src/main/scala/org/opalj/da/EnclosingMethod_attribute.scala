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
case class EnclosingMethod_attribute(
        attribute_name_index: Constant_Pool_Index,
        class_index:          Constant_Pool_Index,
        method_index:         Constant_Pool_Index
) extends Attribute {

    final override def attribute_length = 2 + 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="details enclosing_method_attribute">
            Enclosing method:
            { asJavaObjectType(class_index).asSpan("") }
            {{
            { if (method_index != 0) cp(method_index).toString else "<not immediately enclosed>" }
            }}
        </div>
    }
}
