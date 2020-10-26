/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package classes

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.ClassImmutability

/**
 * This is the basis for the matchers that match a class immutability
 * @author Tobias Peter Roth
 */
class _ClassImmutabilityMatcher(val property: ClassImmutability) extends AbstractPropertyMatcher {

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

class _MutableClassMatcher extends _ClassImmutabilityMatcher(br.fpcf.properties.MutableClass)

class _DependentImmutableClassMatcher extends _ClassImmutabilityMatcher(br.fpcf.properties.DependentImmutableClass)

class _ShallowImmutableClassMatcher extends _ClassImmutabilityMatcher(br.fpcf.properties.ShallowImmutableClass)

class _DeepImmutableClassMatcher extends _ClassImmutabilityMatcher(br.fpcf.properties.DeepImmutableClass)
