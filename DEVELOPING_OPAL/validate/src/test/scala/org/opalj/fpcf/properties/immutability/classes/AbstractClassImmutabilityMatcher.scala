/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package immutability
package classes

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.fpcf.properties.immutability.classes

class AbstractClassImmutabilityMatcher(val property: ClassImmutability) extends AbstractPropertyMatcher {

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
        import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass

        if (!properties.exists(p => p match {
            case DependentlyImmutableClass(_) =>
                val annotationType = a.annotationType.asFieldType.asObjectType
                val parameters =
                    getValue(project, annotationType, a.elementValuePairs, "parameter").
                        asArrayValue.values.map(x => x.asStringValue.value)
                property.isInstanceOf[classes.DependentlyImmutableClass] &&
                    p.asInstanceOf[classes.DependentlyImmutableClass].parameter.size == parameters.size &&
                    parameters.toList.forall(param => p.asInstanceOf[classes.DependentlyImmutableClass].parameter.contains(param))
            case _ => p == property
        })) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class TransitivelyImmutableClassMatcher
    extends AbstractClassImmutabilityMatcher(properties.immutability.TransitivelyImmutableClass)

class DependentlyImmutableClassMatcher
    extends AbstractClassImmutabilityMatcher(properties.immutability.DependentlyImmutableClass(Set.empty))

class NonTransitivelyImmutableClassMatcher
    extends AbstractClassImmutabilityMatcher(properties.immutability.NonTransitivelyImmutableClass)

class MutableClassMatcher extends AbstractPropertyMatcher {
    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (properties.exists {
            case _: MutableClass => true
            case _               => false
        })
            Some(a.elementValuePairs.head.value.asStringValue.value)
        else
            None
    }
}
