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

class AbstractClassImmutabilityMatcher(val property: properties.ClassImmutability) extends AbstractPropertyMatcher {

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
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` ⇒ true
            case _          ⇒ false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class DeepImmutableClassMatcher
    extends AbstractClassImmutabilityMatcher(properties.DeepImmutableClass)

class DependentImmutableClassMatcher
    extends AbstractClassImmutabilityMatcher(properties.DependentImmutableClass)

class ShallowImmutableClassMatcher
    extends AbstractClassImmutabilityMatcher(properties.ShallowImmutableClass)

class MutableClassMatcher extends AbstractPropertyMatcher {
    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (properties.exists {
            case _: MutableClass ⇒ true
            case _               ⇒ false
        })
            Some(a.elementValuePairs.head.value.asStringValue.value)
        else
            None
    }
}
