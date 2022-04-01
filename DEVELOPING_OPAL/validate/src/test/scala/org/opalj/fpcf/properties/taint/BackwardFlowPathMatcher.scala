/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.br._
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.{EPS, Entity, FinalEP, Property}
import org.opalj.tac.fpcf.analyses.ifds.taint.old.BackwardTaintAnalysisFixtureScheduler
import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact}
import org.opalj.tac.fpcf.properties.OldTaint

/**
 * @author Mario Trageser
 */
class BackwardFlowPathMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val method = entity.asInstanceOf[(DefinedMethod, Fact)]._1.definedMethod
        val expectedFlow = a.elementValuePairs.map((evp: ElementValuePair) ⇒
            evp.value.asArrayValue.values.map((value: ElementValue) ⇒
                value.asStringValue.value)).head.toIndexedSeq
        val propertyStore = p.get(PropertyStoreKey)
        val propertyKey = BackwardTaintAnalysisFixtureScheduler.property.key
        val allReachableFlowFacts =
            propertyStore.entities(propertyKey).collect {
                case EPS((m: DefinedMethod, inputFact)) if m.definedMethod == method ⇒
                    (m, inputFact)
            }.flatMap(propertyStore(_, propertyKey) match {
                case FinalEP(_, OldTaint(result)) ⇒
                    result.values.fold(Set.empty)((acc, facts) ⇒ acc ++ facts).collect {
                        case FlowFact(methods) ⇒ methods.map(_.name)
                    }
                case _ ⇒ Seq.empty
            }).toIndexedSeq
        if (expectedFlow.isEmpty) {
            if (allReachableFlowFacts.nonEmpty) return Some(s"There should be no flow for $entity")
            None
        } else {
            if (allReachableFlowFacts.contains(expectedFlow)) None
            else Some(expectedFlow.mkString(", "))
        }
    }
}
