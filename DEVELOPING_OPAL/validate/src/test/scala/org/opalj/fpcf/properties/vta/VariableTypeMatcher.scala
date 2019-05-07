/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.vta

import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.Property
import org.opalj.br.analyses.SomeProject
import org.opalj.br.AnnotationLike
import org.opalj.br.DefinedMethod
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.VariableType
import org.opalj.tac.fpcf.analyses.VTAFact
import org.opalj.tac.fpcf.analyses.VTAResult
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

class VariableTypeMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val expectedPair = a.elementValuePairs.map((evp: ElementValuePair) ⇒
            evp.value.asArrayValue.values.map((value: ElementValue) ⇒
                value.asStringValue.value)).head.toArray
        val method = entity.asInstanceOf[(DefinedMethod, VTAFact)]._1.definedMethod
        val taCode = p.get(PropertyStoreKey)(method, TACAI.key) match {
            case FinalP(TheTACAI(tac)) ⇒ tac
            case _                     ⇒ throw new IllegalStateException("TAC of annotated method not present after analysis")
        }
        val result = properties.filter(_.isInstanceOf[VTAResult]).head
            .asInstanceOf[VTAResult]
            .flows
            .values
            .fold(Set.empty)((acc, facts) ⇒ acc ++ facts)
            .collect {
                case VariableType(definedBy, c) ⇒ Array(taCode.lineNumber(method.body.get, definedBy).get.toString, c.simpleName)
            }
        if (result.exists(_.deep == expectedPair.deep)) None
        else Some(expectedPair.mkString("->"))
    }
}
