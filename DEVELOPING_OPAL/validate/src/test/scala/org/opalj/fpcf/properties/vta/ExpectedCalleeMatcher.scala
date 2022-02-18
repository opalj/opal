/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.vta

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Property
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.AnnotationLike
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.Method
import org.opalj.tac.DUVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.ifds.CalleeType
import org.opalj.tac.fpcf.analyses.ifds.IFDSBasedVariableTypeAnalysis
import org.opalj.tac.fpcf.analyses.ifds.VTAResult

class ExpectedCalleeMatcher extends VTAMatcher {

    def validateSingleAnnotation(project: SomeProject, entity: Entity,
                                 taCode: TACode[TACMethodParameter, DUVar[ValueInformation]],
                                 method: Method, annotation: AnnotationLike,
                                 properties: Traversable[Property]): Option[String] = {
        val elementValuePairs = annotation.elementValuePairs
        val expected = (
            elementValuePairs.head.value.asIntValue.value,
            elementValuePairs(1).value.asStringValue.value,
            elementValuePairs(2).value.asBooleanValue.value
        )
        val propertyStore = project.get(PropertyStoreKey)
        val propertyKey = IFDSBasedVariableTypeAnalysis.property.key
        // Get ALL the exit facts for the method for ALL input facts
        val allReachableExitFacts =
            propertyStore.entities(propertyKey).collect {
                case EPS((m: DefinedMethod, inputFact)) if m.definedMethod == method ⇒
                    (m, inputFact)
            }.flatMap(propertyStore(_, propertyKey) match {
                case FinalEP(_, VTAResult(result)) ⇒
                    result.values.fold(Set.empty)((acc, facts) ⇒ acc ++ facts).collect {
                        case CalleeType(index, t, upperBound) ⇒
                            Seq((
                                taCode.lineNumber(method.body.get, index).get,
                                referenceTypeToString(t), upperBound
                            ))
                    }
                case _ ⇒ Seq.empty
            }).flatten
        if (allReachableExitFacts.contains(expected)) None
        else Some(expected.toString)
    }
}
