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
case class RuntimeVisibleTypeAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        typeAnnotations:      TypeAnnotations
) extends TypeAnnotations_attribute {

    final override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute type_annotations runtime_visible">
            <summary class="attribute_name">Runtime Visible Type Annotations [size: { typeAnnotations.size } item(s)]</summary>
            { typeAnnotationsToXHTML(cp) }
        </details>
    }

}
