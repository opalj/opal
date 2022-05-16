/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * Represents the ''Record'' attribute (Java 16).
 *
 * @author Dominik Helm
 */
case class Record_attribute(
        attribute_name_index: Constant_Pool_Index,
        components:           RecordComponents // ArraySeq[Constant_Pool_Index]
) extends Attribute {

    override def attribute_length: Int = 2 + components.iterator.map(_.length).sum

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute">
            <summary>Record</summary>
            <ol>{ components.map[Node] { c => <li>{ c.toXHTML(cp) }</li> } }</ol>
        </details>
    }

}

case class RecordComponent(
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
) {

    def length: Int = 6 + attributes.iterator.map(_.size).sum

    def toXHTML(implicit cp: Constant_Pool): Seq[Node] = {
        val name = cp(name_index).toString(cp)
        val descriptor = cp(descriptor_index).toString
        <li>{ attributes.map(_.toXHTML) }{ descriptor }<span>{ name }</span></li>
    }

}