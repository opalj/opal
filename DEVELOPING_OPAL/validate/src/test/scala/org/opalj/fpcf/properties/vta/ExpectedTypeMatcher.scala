/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package vta

import org.opalj.br.AnnotationLike
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.ifds.VariableType
import org.opalj.fpcf.ifds.VTAResult
import org.opalj.tac.DUVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.value.ValueInformation

/**
 * Validates expected type annotation for the IFDS based VTA
 *
 * @author Marc Clement
 */
class ExpectedTypeMatcher extends VTAMatcher {

    def validateSingleAnnotation(
        project:    SomeProject,
        entity:     Entity,
        taCode:     TACode[TACMethodParameter, DUVar[ValueInformation]],
        method:     Method,
        annotation: AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val elementValuePairs = annotation.elementValuePairs
        val expected = (
            elementValuePairs.head.value.asIntValue.value,
            elementValuePairs(1).value.asStringValue.value,
            elementValuePairs(2).value.asBooleanValue.value
        )
        val result = properties.filter(_.isInstanceOf[VTAResult]).head
            .asInstanceOf[VTAResult]
            .flows
            .values
            .fold(Set.empty)((acc, facts) => acc ++ facts)
            .collect {
                case VariableType(definedBy, t, upperBound) =>
                    (taCode.lineNumber(method.body.get, definedBy).get, referenceTypeToString(t), upperBound)
            }
        if (result.contains(expected)) None
        else Some(expected.toString)
    }
}
