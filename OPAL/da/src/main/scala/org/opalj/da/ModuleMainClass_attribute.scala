/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * Java 9's `ModuleMainClass` attribute.
 *
 * @author Michael Eichberg
 */
case class ModuleMainClass_attribute(
        attribute_name_index: Constant_Pool_Index,
        main_class_index:     Constant_Pool_Index // CONSTANT_CLASS
) extends Attribute {

    def attribute_length: Int = 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="simple_attribute">
            <span class="attribute_name">MainClass</span>
            :&nbsp;{ cp(main_class_index).toString(cp) }
        </div>
    }
}
