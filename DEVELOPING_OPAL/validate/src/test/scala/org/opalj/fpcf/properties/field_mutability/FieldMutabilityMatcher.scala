/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package field_mutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject

/**
 * Matches a field's `FieldMutability` property. The match is successful if the field has the
 * given property and a sufficiently capable analysis was scheduled.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
class FieldMutabilityMatcher(val property: FieldMutability) extends AbstractPropertyMatcher {

    private final val PropertyReasonID = 0

    final val SupportedAnalyses: Set[ObjectType] = {
        Set(
            ObjectType("org/opalj/fpcf/analyses/L0FieldMutabilityAnalysis"),
            ObjectType("org/opalj/fpcf/analyses/L1FieldMutabilityAnalysis"),
            ObjectType("org/opalj/fpcf/analyses/L2FieldMutabilityAnalysis")
        )
    }

    override def isRelevant(
        p:      SomeProject,
        as:     Set[ObjectType],
        entity: Object,
        a:      AnnotationLike
    ): Boolean = {
        as.exists(SupportedAnalyses.contains)
    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)
        if (analyses.exists(as.contains) && !properties.exists(p ⇒ p == property)) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }

}

class DeclaredFinalMatcher extends FieldMutabilityMatcher(DeclaredFinalField)

class EffectivelyFinalMatcher extends FieldMutabilityMatcher(EffectivelyFinalField)

class LazyInitializedMatcher extends FieldMutabilityMatcher(LazyInitializedField)