/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package fields

import scala.collection.immutable.SortedSet

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.FieldImmutability

/**
 * Matches a field's `FieldImmutability` property. The match is successful if the field has the
 * given property and a sufficiently capable analysis was scheduled.
 *
 * @author Tobias Roth
 * @author Michael Eichberg
 * @author Dominik Helm
 */
class FieldImmutabilityMatcher(val property: FieldImmutability) extends AbstractPropertyMatcher {

    // private final val PropertyReasonID = 0

    override def isRelevant(
        p:      SomeProject,
        as:     Set[ObjectType],
        entity: Object,
        a:      AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asObjectType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        analyses.exists(as.contains)
    }

    def validateProperty(
        project:    SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {

        if (!properties.exists(p => p == property)) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(0).value.asStringValue.value)
        } else {
            None
        }
    }
}

class MutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.immutability.MutableField)

class NonTransitiveImmutableFieldMatcher
    extends FieldImmutabilityMatcher(br.fpcf.properties.immutability.NonTransitivelyImmutableField)

class DependentlyImmutableFieldMatcher
    extends FieldImmutabilityMatcher(br.fpcf.properties.immutability.DependentlyImmutableField(SortedSet.empty)) {
    override def validateProperty(
        project:    SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {

        if (!properties.exists(p =>
                p match {
                    case DependentlyImmutableField(annotationParameters) =>
                        val annotationType = a.annotationType.asFieldType.asObjectType
                        val analysisParameters =
                            getValue(project, annotationType, a.elementValuePairs, "parameter").asArrayValue.values.map(
                                x =>
                                    x.asStringValue.value
                            )
                        annotationParameters.equals(analysisParameters.toSet)
                    case _ => p == property
                }
            )
        ) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(0).value.asStringValue.value)
        } else {
            None
        }
    }
}

class TransitiveImmutableFieldMatcher
    extends FieldImmutabilityMatcher(br.fpcf.properties.immutability.TransitivelyImmutableField)
