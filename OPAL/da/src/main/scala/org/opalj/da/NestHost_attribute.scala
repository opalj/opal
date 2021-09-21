/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * Java 11's `ModuleMainClass` attribute.
 *
 * @author Dominik Helm
 */
case class NestHost_attribute(
        attribute_name_index: Constant_Pool_Index,
        host_class_index:     Constant_Pool_Index // CONSTANT_CLASS
) extends Attribute {

    def attribute_length: Int = 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="simple_attribute">
            <span class="attribute_name">NestHost</span>
            :&nbsp;{ cp(host_class_index).toString(cp) }
        </div>
    }
}
