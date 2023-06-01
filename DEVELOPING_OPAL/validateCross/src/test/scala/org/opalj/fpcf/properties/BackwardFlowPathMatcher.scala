/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties

import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.ifds.BackwardTaintAnalysisFixtureScheduler
import org.opalj.ifds.IFDSFact
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.properties.Taint

class BackwardFlowPathMatcher extends AbstractBackwardFlowPathMatcher(BackwardTaintAnalysisFixtureScheduler.property.key)

/**
 * @author Mario Trageser
 */
abstract class AbstractBackwardFlowPathMatcher(pk: PropertyKey[_ <: Property]) extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val method = entity.asInstanceOf[(Method, IFDSFact[TaintFact, JavaStatement])]._1
        val expectedFlow = a.elementValuePairs.map((evp: ElementValuePair) =>
            evp.value.asArrayValue.values.map((value: ElementValue) =>
                value.asStringValue.value)).head
        val propertyStore = p.get(PropertyStoreKey)
        val allReachableFlowFacts = propertyStore.entities(pk)
            .collect {
                case EPS((m: Method, inputFact)) if m == method => (m, inputFact)
            }.flatMap(propertyStore(_, pk) match {
                case FinalEP(_, Taint(result, _)) =>
                    result.values.fold(Set.empty)((acc, facts) => acc ++ facts).collect {
                        case FlowFact(methods) => methods.map(_.name)
                    }
                case _ => Seq.empty
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