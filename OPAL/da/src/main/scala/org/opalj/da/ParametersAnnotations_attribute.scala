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
trait ParametersAnnotations_attribute extends Attribute {

    final override def attribute_length: Int = {
        parameters_annotations.foldLeft(1 /*num_parameters*/ ) { (c, n) =>
            c + n.foldLeft(2 /*num_annotations*/ )((c, n) => c + n.attribute_length)
        }
    }

    def parameters_annotations: ParametersAnnotations

    def parametersAnnotationstoXHTML(implicit cp: Constant_Pool): Node = {
        val ans = {
            for { // TODO This doesn't make sense: it is no longer possible to distinguish parameters
                (perParameterAnnotations, parameterIndex) <- parameters_annotations.zipWithIndex
                annotation <- perParameterAnnotations
            } yield annotation.toXHTML(cp)
        }

        <div>{ ans }</div>
    }
}
