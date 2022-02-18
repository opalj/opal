/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValuePair
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ElementValue
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Taint

/**
 * @author Mario Trageser
 */
class ForwardFlowPathMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val expectedFlow = a.elementValuePairs.map((evp: ElementValuePair) ⇒
            evp.value.asArrayValue.values.map((value: ElementValue) ⇒
                value.asStringValue.value)).head.toIndexedSeq
        val flows = properties.filter(_.isInstanceOf[Taint]).head
            .asInstanceOf[Taint]
            .flows
            .values
            .fold(Set.empty)((acc, facts) ⇒ acc ++ facts)
            .collect {
                case FlowFact(methods) ⇒ methods.map(_.name).toIndexedSeq
            }
        if (expectedFlow.isEmpty) {
            if (flows.nonEmpty) return Some(s"There should be no flow for $entity")
            None
        } else {
            if (flows.contains(expectedFlow)) None
            else Some(expectedFlow.mkString(", "))
        }
    }
}
