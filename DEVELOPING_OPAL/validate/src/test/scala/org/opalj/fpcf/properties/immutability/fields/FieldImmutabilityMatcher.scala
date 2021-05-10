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
 * Matches a field's `FieldImmutability` property. The match is successful if the field has the
 * given property and a sufficiently capable analysis was scheduled.
 *
 * @author Tobias Roth
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
        project:    SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        import org.opalj.br.fpcf.properties.DependentlyImmutableField
        if (!properties.exists(p ⇒ p match {
            case DependentlyImmutableField(_) ⇒
                val annotationType = a.annotationType.asFieldType.asObjectType
                val parameters =
                    getValue(project, annotationType, a.elementValuePairs, "parameter").
                        asArrayValue.values.map(x ⇒ x.asStringValue.value)
                property.isInstanceOf[DependentlyImmutableField] &&
                    p.asInstanceOf[DependentlyImmutableField].parameter.size == parameters.size &&
                    parameters.toList.forall(param ⇒ p.asInstanceOf[DependentlyImmutableField].parameter.contains(param))
            case _ ⇒ p == property
        })) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }
}

class NonTransitiveImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.NonTransitivelyImmutableField)

class DependentImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.DependentlyImmutableField(Set.empty))

class TransitiveImmutableFieldMatcher extends FieldImmutabilityMatcher(br.fpcf.properties.TransitivelyImmutableField)

