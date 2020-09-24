/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.fields

import org.opalj.br
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher
/*import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.LazyInitializedField*/

/**
 * Matches a field's `FieldImmutability` property. The match is successful if the field has the
 * given property and a sufficiently capable analysis was scheduled.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
class FieldImmutabilityMatcher(val property: FieldImmutability) extends AbstractPropertyMatcher {

    private final val PropertyReasonID = 0

    override def isRelevant(
        p:      SomeProject,
        as:     Set[ObjectType],
        entity: Object,
        a:      AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asObjectType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)

        analyses.exists(as.contains)
    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (!properties.exists(p ⇒ p == property)) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }

}
/*
class DeclaredFinalMatcher extends FieldImmutabilityMatcher(ShallowImmutableField)

class EffectivelyFinalMatcher extends FieldImmutabilityMatcher(ShallowImmutableField) */

//class MutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.MutableField)
class ShallowImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.ShallowImmutableField) //ShallowImmutableField)

class DependentImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.ShallowImmutableField)
class DeepImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.ShallowImmutableField)

