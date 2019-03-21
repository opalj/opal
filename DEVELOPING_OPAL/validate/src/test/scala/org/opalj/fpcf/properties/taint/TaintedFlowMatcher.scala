/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{AnnotationLike, ElementValuePair, ObjectType}
import org.opalj.fpcf.{Entity, Property}
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.tac.fpcf.analyses.{FlowFact, Taint}

class TaintedFlowMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val taintProperty = properties.filter(_.isInstanceOf[Taint])
        val expectedFlow = a.elementValuePairs.map((evp: ElementValuePair) ⇒ evp.value.asStringValue.value).head
        if (taintProperty.isEmpty) Some(expectedFlow.mkString(", "))
        else {
            val flows = taintProperty.head
                .asInstanceOf[Taint]
                .flows
                .values
                .fold(Set.empty)((acc, facts) ⇒ acc ++ facts)
                .map(_ match {
                    case FlowFact(methods) ⇒ methods.map(_.name).mkString(",")
                    case _                 ⇒ ""
                })
                .filter(!_.isEmpty)
            if (flows.exists(_ == expectedFlow)) None
            else Some(expectedFlow.mkString(", "))
        }
    }
}
