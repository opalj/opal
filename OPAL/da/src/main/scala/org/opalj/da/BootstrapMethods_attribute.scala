/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 */
case class BootstrapMethods_attribute(
        attribute_name_index: Constant_Pool_Index,
        bootstrap_methods:    Seq[BootstrapMethod]
) extends Attribute {

    override def attribute_length: Int = {
        2 /* num_bootstrap_methods */ + bootstrap_methods.view.map(_.size).sum
    }

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute">
            <summary class="attribute_name">BootstrapMethods</summary>
            { methodsToXHTML(cp) }
        </details>
    }

    def methodsToXHTML(implicit cp: Constant_Pool): Seq[Node] = bootstrap_methods.map(_.toXHTML(cp))
}
