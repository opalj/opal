/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * Represents the ''ModulePackages'' attribute (Java 9).
 *
 * @author Michael Eichberg
 */
case class ModulePackages_attribute(
        attribute_name_index: Constant_Pool_Index,
        package_index_table:  IndexedSeq[Constant_Pool_Index]
) extends Attribute {

    override def attribute_length: Int = 2 + package_index_table.size * 2

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute">
            <summary>ModulePackages</summary>
            { package_index_table.map(p ⇒ cp(p).toString).sorted.map { p ⇒ <span>{ p }</span><br/> } }
        </details>
    }

}
