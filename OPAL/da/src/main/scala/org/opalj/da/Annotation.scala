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
case class Annotation(
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs   = NoElementValuePairs
) extends AbstractAnnotation {

    final def attribute_length: Int = {
        2 + element_value_pairs.foldLeft(2 /*num_...*/ )((c, n) => c + n.attribute_length)
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val annotationType = parseFieldType(cp(type_index).toString)
        <div class="annotation">
            { annotationType.asSpan("annotation_type") }
            { evps }
        </div>
    }
}
