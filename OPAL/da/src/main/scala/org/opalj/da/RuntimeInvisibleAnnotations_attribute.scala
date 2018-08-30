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
case class RuntimeInvisibleAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
) extends Annotations_attribute {

    final override def attribute_length: Int = annotations.foldLeft(2 /*count*/ )(_ + _.attribute_length)

    final override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute annotations runtime_invisible">
            <summary class="attribute_name">Runtime Invisible Annotations [size: { annotations.size } item(s)]</summary>
            { annotationsToXHTML(cp) }
        </details>
    }

}
