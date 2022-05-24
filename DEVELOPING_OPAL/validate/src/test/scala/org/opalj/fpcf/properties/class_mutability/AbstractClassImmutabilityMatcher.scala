/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package class_mutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties

class AbstractClassImmutabilityMatcher(
        val property: properties.ClassImmutability
) extends AbstractPropertyMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` => true
            case _          => false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class ImmutableObjectMatcher
    extends AbstractClassImmutabilityMatcher(properties.ImmutableObject)

class ImmutableContainerObjectMatcher
    extends AbstractClassImmutabilityMatcher(properties.ImmutableContainer)

class MutableObjectMatcher extends AbstractPropertyMatcher {
    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (properties.exists {
            case _: MutableObject => true
            case _                => false
        })
            Some(a.elementValuePairs.head.value.asStringValue.value)
        else
            None
    }
}
