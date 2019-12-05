/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.field_immutability

import org.opalj.br.{AnnotationLike, ObjectType}
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

/**
 * @author Tobias Peter Roth
 */
class FieldImmutabilityMatcher(val property: FieldImmutability) extends AbstractPropertyMatcher {

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
            p match {
                case DependentImmutableField(_) if property == DependentImmutableField() ⇒ true
                case _ if p == property ⇒ true
                case _ ⇒ false
            }
        })) { //p == property)) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }

}

class MutableFieldMatcher extends FieldImmutabilityMatcher(MutableField)

class ShallowImmutableFieldMatcher extends FieldImmutabilityMatcher(ShallowImmutableField)

class DependentImmutableFieldMatcher extends FieldImmutabilityMatcher(DependentImmutableField())

class DeepImmutableFieldMatcher extends FieldImmutabilityMatcher(DeepImmutableField)
