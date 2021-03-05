/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.DefinedMethod
import org.opalj.tac.fpcf.analyses.ifds.taint.BackwardTaintAnalysisFixture
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Taint

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
        val propertyKey = BackwardTaintAnalysisFixture.property.key
        val allReachableFlowFacts =
            propertyStore.entities(propertyKey).collect {
                case EPS((m: DefinedMethod, inputFact)) if m.definedMethod == method ⇒
                    (m, inputFact)
            }.flatMap(propertyStore(_, propertyKey) match {
                case FinalEP(_, Taint(result)) ⇒
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
