/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.types

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

class AbstractTypeImmutabilityMatcher(
        val property: TypeImmutability
) extends AbstractPropertyMatcher {

    import org.opalj.br.analyses.SomeProject

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

    override def validateProperty(
        project:    Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        import org.opalj.br.fpcf.properties.DependentlyImmutableType
        if (!properties.exists(p ⇒ p match {
            case DependentlyImmutableType(_) ⇒
                val annotationType = a.annotationType.asFieldType.asObjectType
                val parameters =
                    getValue(project, annotationType, a.elementValuePairs, "parameter").
                        asArrayValue.values.map(x ⇒ x.asStringValue.value)
                property.isInstanceOf[DependentlyImmutableType] &&
                    p.asInstanceOf[DependentlyImmutableType].parameter.size == parameters.size &&
                    parameters.toList.forall(param ⇒ p.asInstanceOf[DependentlyImmutableType].parameter.contains(param))
            case _ ⇒ p == property
        })) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class TransitiveImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.TransitivelyImmutableType)
class DependentImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.DependentlyImmutableType(Set.empty))
class NonTransitiveImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.NonTransitivelyImmutableType)
class MutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.MutableType)

