/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class StackMapTable_attribute(
        attribute_name_index: Constant_Pool_Index,
        stack_map_frames:     StackMapFrames
) extends Attribute {

    final override def attribute_length: Int = {
        stack_map_frames.foldLeft(2 /*count*/ )((c, n) => c + n.attribute_length)
    }

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div>
            <details class="attribute">
                <summary class="attribute_name">StackMapTable [size: { stack_map_frames.length } item(s)]</summary>
                { stack_map_framestoXHTML(cp) }
            </details>
        </div>
    }

    def stack_map_framestoXHTML(implicit cp: Constant_Pool): Node = {
        var offset: Int = -1
        val framesAsXHTML =
            for (stack_map_frame <- stack_map_frames) yield {
                val (frameAsXHTML, newOffset) = stack_map_frame.toXHTML(cp, offset)
                offset = newOffset
                frameAsXHTML
            }
        <table class="stack_map_table">
            <thead>
                <tr><th>PC</th><th>Kind</th><th>Frame Type</th><th>Offset Delta</th><th>Details</th></tr>
            </thead>
            <tbody>
                { framesAsXHTML }
            </tbody>
        </table>
    }

}
