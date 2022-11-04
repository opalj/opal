/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package thrown_exceptions

import org.opalj.br.analyses.SomeProject
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.ThrownExceptionsByOverridingMethods

/**
 * Matches a methods's `ThrownExceptionsByOverridingMethods` property.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
class ExpectedExceptionsByOverridingMethodsMatcher
    extends AbstractPropertyMatcher
    with ExceptionTypeExtractor {

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
        val requiredAnalysis = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        val isPropertyValid = !requiredAnalysis.exists(as.contains) ||
            properties.forall {
                case ate: ThrownExceptionsByOverridingMethods =>
                    ate.exceptions.nonEmpty &&
                        concreteTypeExceptions.forall(ate.exceptions.concreteTypes.contains(_)) &&
                        upperBoundTypeExceptions.forall(ate.exceptions.upperTypeBounds.contains(_))
                case _ => true
            }

        if (isPropertyValid) {
            None
        } else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
