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
case class TypeAnnotation(
        target_type:         TypeAnnotationTarget,
        target_path:         TypeAnnotationPath,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
) extends AbstractAnnotation {

    final def attribute_length: Int = {
        target_type.attribute_length +
            target_path.attribute_length +
            2 +
            element_value_pairs.foldLeft(2 /*num_...*/ )(_ + _.attribute_length)
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="type_annotation">
            { target_type.toXHTML }
            <div>
                { target_path.toXHTML }
                <b>Annotation Type</b>
                { parseFieldType(type_index).asSpan("") }<br/>
                { evps }
            </div>
        </div>
    }
}
