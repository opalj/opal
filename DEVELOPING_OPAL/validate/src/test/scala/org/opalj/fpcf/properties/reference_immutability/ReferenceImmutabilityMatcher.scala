/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.reference_immutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeOrNotDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeReference
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

/**
 * @author Tobias Peter Roth
 */
class ReferenceImmutabilityMatcher(val property: ReferenceImmutability)
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
        if (!properties.exists(p ⇒ {
            val tmpP = {
                p match {
                    //case ImmutableReference(_) ⇒ ImmutableReference(true)
                    case _ ⇒ p
                }
            }
            tmpP == property
        })) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }
}

class LazyInitializedThreadSafeReferenceMatcher extends ReferenceImmutabilityMatcher(LazyInitializedThreadSafeReference)

class LazyInitializedNotThreadSafeButDeterministicReferenceMatcher extends ReferenceImmutabilityMatcher(LazyInitializedNotThreadSafeButDeterministicReference)

class LazyInitializedNotThreadSafeOrNotDeterministicReferenceMatcher extends ReferenceImmutabilityMatcher(LazyInitializedNotThreadSafeOrNotDeterministicReference)

class ImmutableReferenceMatcher extends ReferenceImmutabilityMatcher(ImmutableReference)
