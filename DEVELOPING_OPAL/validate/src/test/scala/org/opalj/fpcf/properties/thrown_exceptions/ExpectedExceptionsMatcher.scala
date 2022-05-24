/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package thrown_exceptions

import org.opalj.br.analyses.SomeProject
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.ThrownExceptions

import scala.collection.immutable.ArraySeq

/**
 * Trait to extract the concrete and upper bound exceptions specified in the test cases.
 *
 * @author Andreas Muttscheller
 */
private[thrown_exceptions] trait ExceptionTypeExtractor extends AbstractPropertyMatcher {

    def getConcreteAndUpperBoundExceptionAnnotations(
        p: SomeProject,
        a: AnnotationLike
    ): (ArraySeq[ObjectType], ArraySeq[ObjectType]) = {
        val annotationType = a.annotationType.asObjectType
        val exceptionTypesAnnotation = getValue(
            p,
            annotationType,
            a.elementValuePairs,
            "value"
        ).asAnnotationValue.annotation

        val concreteTypeExceptions = getValue(
            p,
            exceptionTypesAnnotation.annotationType.asObjectType,
            exceptionTypesAnnotation.elementValuePairs,
            "concrete"
        ).asArrayValue
        val upperBoundTypeExceptions = getValue(
            p,
            exceptionTypesAnnotation.annotationType.asObjectType,
            exceptionTypesAnnotation.elementValuePairs,
            "upperBound"
        ).asArrayValue

        (
            concreteTypeExceptions.values.map[ObjectType](ev => ev.asClassValue.value.asObjectType),
            upperBoundTypeExceptions.values.map[ObjectType](ev => ev.asClassValue.value.asObjectType)
        )
    }
}

/**
 * Matches a methods's `ThrownExceptions` property.
 *
 * @author Andreas Muttscheller
 */
class ExpectedExceptionsMatcher extends AbstractPropertyMatcher with ExceptionTypeExtractor {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val (concreteTypeExceptions, upperBoundTypeExceptions) =
            getConcreteAndUpperBoundExceptionAnnotations(p, a)

        val annotationType = a.annotationType.asObjectType
        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "requires").asArrayValue.values
        val requiredAnalysis = analysesElementValues.map[ObjectType](ev => ev.asClassValue.value.asObjectType)

        val isPropertyValid = !requiredAnalysis.exists(as.contains) ||
            properties.forall {
                case t: ThrownExceptions =>
                    t.types.nonEmpty &&
                        concreteTypeExceptions.forall(t.types.concreteTypes.contains(_)) &&
                        upperBoundTypeExceptions.forall(t.types.upperTypeBounds.contains(_))
                case _ => true
            }

        if (isPropertyValid) {
            None
        } else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
