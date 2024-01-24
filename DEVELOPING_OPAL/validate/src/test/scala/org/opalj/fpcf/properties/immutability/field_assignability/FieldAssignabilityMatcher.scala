/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package field_assignability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized

/**
 * @author Tobias Roth
 */
class FieldAssignabilityMatcher(val property: FieldAssignability)
    extends AbstractPropertyMatcher {

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
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        analyses.exists(as.contains)
    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists(p => p == property)) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }
}

class AssignableFieldMatcher extends FieldAssignabilityMatcher(Assignable)

class LazilyInitializedFieldMatcher extends FieldAssignabilityMatcher(LazilyInitialized)

class UnsafelyLazilyInitializedFieldMatcher extends FieldAssignabilityMatcher(UnsafelyLazilyInitialized)

class EffectivelyNonAssignableFieldMatcher extends FieldAssignabilityMatcher(EffectivelyNonAssignable)

class NonAssignableFieldMatcher extends FieldAssignabilityMatcher(NonAssignable)
