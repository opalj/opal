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
case class RuntimeInvisibleTypeAnnotations_attribute(
    attribute_name_index: Constant_Pool_Index,
    typeAnnotations:      TypeAnnotations
) extends TypeAnnotations_attribute {

    override final def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="attribute type_annotations runtime_invisible">
            <summary class="attribute_name">Runtime Invisible Type Annotations [size: {
                typeAnnotations.size
            } item(s)]</summary>
            {typeAnnotationsToXHTML(cp)}
        </details>
    }

}
