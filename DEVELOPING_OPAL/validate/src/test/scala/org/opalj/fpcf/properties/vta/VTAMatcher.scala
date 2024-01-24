/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package vta

import org.opalj.br.AnnotationLike
import org.opalj.br.ArrayType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.Property
import org.opalj.fpcf.ifds.VTAFact
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.tac.DUVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.value.ValueInformation

/**
 * Super class for validation of the IFDS based VTA result annotations
 *
 * @author Marc Clement
 */
abstract class VTAMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val method = entity.asInstanceOf[(Method, VTAFact)]._1
        val taCode = p.get(PropertyStoreKey)(method, TACAI.key) match {
            case FinalP(TheTACAI(tac)) => tac
            case _ =>
                throw new IllegalStateException(
                    "TAC of annotated method not present after analysis"
                )
        }
        val result = a.elementValuePairs(0).value.asArrayValue.values
            .map(annotationValue =>
                validateSingleAnnotation(
                    p,
                    entity,
                    taCode,
                    method,
                    annotationValue.asAnnotationValue.annotation,
                    properties
                )
            ).filter(_.isDefined)
        if (result.isEmpty) None
        else Some(result.map(_.get).mkString(", "))
    }

    def validateSingleAnnotation(
        project:    SomeProject,
        entity:     Entity,
        taCode:     TACode[TACMethodParameter, DUVar[ValueInformation]],
        method:     Method,
        annotation: AnnotationLike,
        properties: Iterable[Property]
    ): Option[String]

    def referenceTypeToString(t: ReferenceType): String = t match {
        case objectType: ObjectType => objectType.simpleName
        case arrayType: ArrayType =>
            referenceTypeToString(arrayType.elementType.asReferenceType) + "[]"
    }
}
