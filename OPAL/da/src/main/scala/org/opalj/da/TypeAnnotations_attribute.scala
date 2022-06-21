/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 */
trait TypeAnnotations_attribute extends Attribute {

    val typeAnnotations: TypeAnnotations

    final override def attribute_length: Int = {
        typeAnnotations.foldLeft(2 /*count*/ )(_ + _.attribute_length)
    }

    def typeAnnotationsToXHTML(implicit cp: Constant_Pool): Node = {
        <ul class="annotations">
            { typeAnnotations.map(ta => <li>{ ta.toXHTML }</li>) }
        </ul>
    }
}
