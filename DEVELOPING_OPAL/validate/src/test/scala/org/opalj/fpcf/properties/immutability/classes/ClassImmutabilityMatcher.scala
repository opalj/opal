/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package classes

import scala.collection.immutable.SortedSet

import org.opalj.br.AnnotationLike
import org.opalj.br.ClassType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass

class ClassImmutabilityMatcher(val property: ClassImmutability) extends AbstractPropertyMatcher {

    import org.opalj.br.analyses.SomeProject

    override def isRelevant(
        p:      SomeProject,
        as:     Set[ClassType],
        entity: Object,
        a:      AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asClassType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asClassType)

        analyses.exists(as.contains)
    }

    override def validateProperty(
        project:    Project[?],
        as:         Set[ClassType],
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

class TransitivelyImmutableClassMatcher extends ClassImmutabilityMatcher(TransitivelyImmutableClass)

class DependentlyImmutableClassMatcher
    extends ClassImmutabilityMatcher(DependentlyImmutableClass(SortedSet.empty)) {
    override def validateProperty(
        project:    Project[?],
        as:         Set[ClassType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
                case DependentlyImmutableClass(latticeParameters) =>
                    val annotationType = a.annotationType.asFieldType.asClassType
                    val annotationParameters =
                        getValue(project, annotationType, a.elementValuePairs, "parameter").asArrayValue.values.map(x =>
                            x.asStringValue.value
                        )
                    annotationParameters.toSet.equals(latticeParameters.toSet)
                case p => p == property
            }
        ) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class NonTransitivelyImmutableClassMatcher extends ClassImmutabilityMatcher(NonTransitivelyImmutableClass)

class MutableClassMatcher extends ClassImmutabilityMatcher(MutableClass)
