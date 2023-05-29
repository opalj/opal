/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties

import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.properties.Taint

/**
 * @author Mario Trageser
 */
class ForwardFlowPathMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedFlow = a.elementValuePairs.map((evp: ElementValuePair) =>
            evp.value.asArrayValue.values.map((value: ElementValue) =>
                value.asStringValue.value)).head
        val flows = properties.flatMap {
            case Taint(flows, _) => flows.values.flatten.collect {
                case FlowFact(methods) => methods.map(_.name).toIndexedSeq
            }
            case _ => IndexedSeq.empty
        }.toSet
        if (expectedFlow.isEmpty) {
            if (flows.nonEmpty)
                Some(s"There should be no flow for $entity")
            else
                None
        } else {
            if (flows.contains(expectedFlow))
                None
            else
                Some(expectedFlow.mkString(", "))
        }
    }
}
