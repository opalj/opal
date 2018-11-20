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
trait Annotations_attribute extends Attribute {

    val annotations: Annotations

    def annotationsToXHTML(implicit cp: Constant_Pool): Seq[Node] = annotations.map(_.toXHTML)

}
