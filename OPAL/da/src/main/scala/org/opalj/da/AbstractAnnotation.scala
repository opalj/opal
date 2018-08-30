/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Text
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 */
abstract class AbstractAnnotation {

    def element_value_pairs: ElementValuePairs

    def evps(implicit cp: Constant_Pool): NodeSeq =
        if (element_value_pairs.nonEmpty) {
            val evpsAsXHTML = this.element_value_pairs.map(_.toXHTML)
            NodeSeq.fromSeq(Seq(
                Text("("),
                <ol class="element_value_pairs">{ evpsAsXHTML }</ol>,
                Text(")")
            ))
        } else {
            NodeSeq.Empty
        }
}
