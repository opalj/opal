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
case class SourceDebugExtension_attribute(
        attribute_name_index: Constant_Pool_Index,
        debug_extension:      Array[Byte]
) extends Attribute {

    final override def attribute_length = debug_extension.length

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <span><span class="attribute_name">SourceDebugExtension</span>:{ byteArrayToNode(debug_extension) }</span>
    }

}
