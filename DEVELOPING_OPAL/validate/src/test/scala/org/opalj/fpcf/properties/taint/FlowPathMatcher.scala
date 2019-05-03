/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{AnnotationLike, ElementValuePair, ObjectType}
import org.opalj.fpcf.{Entity, Property}
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.br.ElementValue
import org.opalj.tac.fpcf.analyses.{FlowFact, Taint}

/**
 * @author Mario Trageser
 */
class FlowPathMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val expectedFlow = a.elementValuePairs.map((evp: ElementValuePair) ⇒
            evp.value.asArrayValue.values.map((value: ElementValue) ⇒
                value.asStringValue.value)).head
        val flows = properties.filter(_.isInstanceOf[Taint]).head
            .asInstanceOf[Taint]
            .flows
            .values
            .fold(Set.empty)((acc, facts) ⇒ acc ++ facts)
            .collect {
                case FlowFact(methods) ⇒ methods.map(_.name)
            }
        if (expectedFlow.isEmpty) {
            if (!flows.isEmpty) return Some(s"There should be no flow for $entity")
            return None
        } else {
            if (flows.exists(_ == expectedFlow)) None
            else Some(expectedFlow.mkString(", "))
        }
    }
}
