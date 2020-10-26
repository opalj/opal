/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package fields

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.FieldImmutability

/**
 * This is the basis for the matchers that match the immutability of a field
 * @author Tobias Peter Roth
 */
class _FieldImmutabilityMatcher(val property: FieldImmutability) extends AbstractPropertyMatcher {

    final private val PropertyReasonID = 0

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

class _MutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.MutableField)

class _ShallowImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.ShallowImmutableField)

class _DependentImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.DependentImmutableField)

class _DeepImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.DeepImmutableField)
