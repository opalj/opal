/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{AnnotationLike, ElementValuePair, ObjectType}
import org.opalj.fpcf.{Entity, Property}
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.br.ElementValue
import org.opalj.tac.fpcf.analyses.{FlowFact, Taint}

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
        val taintProperty = properties.filter(_.isInstanceOf[Taint])
        if (taintProperty.isEmpty) Some(expectedFlow.mkString(", "))
        else {
            val flows = taintProperty.head
                .asInstanceOf[Taint]
                .flows
                .values
                .fold(Set.empty)((acc, facts) ⇒ acc ++ facts)
                .map(_ match {
                    case FlowFact(methods) ⇒ methods.map(_.name)
                    case _                 ⇒ Seq.empty
                })
                .filter(!_.isEmpty)
            if (flows.exists(_ == expectedFlow)) None
            else Some(expectedFlow.mkString(", "))
        }
    }
}
