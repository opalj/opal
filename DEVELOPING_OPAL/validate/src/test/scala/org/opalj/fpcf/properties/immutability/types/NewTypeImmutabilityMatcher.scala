/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package types

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.TypeImmutability_new

/**
 * This is the basis for the matchers that match the immutability of a type
 * @author Tobias Peter Roth
 */
class NewTypeImmutabilityMatcher(val property: TypeImmutability_new)
    extends AbstractPropertyMatcher {

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
class NewMutableTypeMatcher extends NewTypeImmutabilityMatcher(br.fpcf.properties.MutableType_new)

class ShallowImmutableTypeMatcher extends NewTypeImmutabilityMatcher(br.fpcf.properties.ShallowImmutableType)

class DependentImmutableTypeMatcher extends NewTypeImmutabilityMatcher(br.fpcf.properties.DependentImmutableType)

class DeepImmutableTypeMatcher extends NewTypeImmutabilityMatcher(br.fpcf.properties.DeepImmutableType)
