/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.immutability.classes

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

class AbstractClassImmutabilityMatcher(
        val property: properties.ClassImmutability
) extends AbstractPropertyMatcher {

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
            case _: MutableClass ⇒ true // MutableObject ⇒ true
            case _               ⇒ false
        })
            Some(a.elementValuePairs.head.value.asStringValue.value)
        else
            None
    }
}
