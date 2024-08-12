/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package types

import scala.collection.immutable.SortedSet

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

class TypeImmutabilityMatcher(
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
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        analyses.exists(as.contains)
    }

    override def validateProperty(
        project:    Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists(p => p == property)) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class TransitiveImmutableTypeMatcher
    extends TypeImmutabilityMatcher(org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType)

class DependentlyImmutableTypeMatcher
    extends TypeImmutabilityMatcher(org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType(SortedSet.empty)) {
    override def validateProperty(
        project:    Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists(p =>
                p match {
                    case DependentlyImmutableType(latticeParameters) =>
                        val annotationType = a.annotationType.asFieldType.asObjectType
                        val annotationParameters =
                            getValue(project, annotationType, a.elementValuePairs, "parameter").asArrayValue.values
                                .map(x => x.asStringValue.value)
                        annotationParameters.toSet.equals(latticeParameters.toSet)
                    case _ => p == property
                }
            )
        ) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }

}

class NonTransitiveImmutableTypeMatcher
    extends TypeImmutabilityMatcher(org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType)

class MutableTypeMatcher
    extends TypeImmutabilityMatcher(org.opalj.br.fpcf.properties.immutability.MutableType)
