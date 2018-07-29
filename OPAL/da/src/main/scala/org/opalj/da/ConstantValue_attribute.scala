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
case class ConstantValue_attribute(
        attribute_name_index: Constant_Pool_Index,
        constantValue_index:  Constant_Pool_Index
) extends Attribute {

    final override def attribute_length = 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <span>=<span class="constant_value"> { cp(constantValue_index).asInstructionParameter }</span></span>
    }

}
