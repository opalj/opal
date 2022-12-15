/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{AnnotationLike, ElementValue, ElementValuePair, ObjectType}
import org.opalj.fpcf.ifds.IFDSProperty
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.{Entity, Property}
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact

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
                value.asStringValue.value)).head.toIndexedSeq
        val flows = properties.filter(_.isInstanceOf[IFDSProperty[_, _]]).head
            .asInstanceOf[IFDSProperty[_, _]]
            .flows
            .values
            .fold(Set.empty)((acc, facts) => acc ++ facts)
            .collect {
                case FlowFact(methods) => methods.map(_.name).toIndexedSeq
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
