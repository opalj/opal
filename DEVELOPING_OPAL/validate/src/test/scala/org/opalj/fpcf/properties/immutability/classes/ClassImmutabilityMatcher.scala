/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package classes

import scala.collection.immutable.SortedSet

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass

class ClassImmutabilityMatcher(val property: ClassImmutability) extends AbstractPropertyMatcher {

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

class TransitivelyImmutableClassMatcher
    extends ClassImmutabilityMatcher(properties.immutability.TransitivelyImmutableClass)

class DependentlyImmutableClassMatcher
    extends ClassImmutabilityMatcher(properties.immutability.DependentlyImmutableClass(SortedSet.empty)) {
    override def validateProperty(
        project:    Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists(p =>
                p match {
                    case DependentlyImmutableClass(latticeParameters) =>
                        val annotationType = a.annotationType.asFieldType.asObjectType
                        val annotationParameters =
                            getValue(project, annotationType, a.elementValuePairs, "parameter").asArrayValue.values.map(
                                x =>
                                    x.asStringValue.value
                            )
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

class NonTransitivelyImmutableClassMatcher
    extends ClassImmutabilityMatcher(properties.immutability.NonTransitivelyImmutableClass)

class MutableClassMatcher extends ClassImmutabilityMatcher(properties.immutability.MutableClass)
