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
case class RuntimeInvisibleParameterAnnotations_attribute(
        attribute_name_index: Constant_Pool_Index,
        parameters_annotations: ParametersAnnotations) extends ParametersAnnotations_attribute {

    override final def toXHTML(implicit cp: Constant_Pool): Node =
        <details class="attribute parameter_annotations runtime_invisible">
            <summary class="attribute">Runtime Invisible Parameter Annotations [size: {
            parameters_annotations.size
        } item(s)]</summary>
            {parametersAnnotationstoXHTML(cp)}
        </details>

}
