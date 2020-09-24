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

class DeepImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.DeepImmutableType)
class DependentImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.DependentImmutableType)
class ShallowImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.ShallowImmutableType)
class MutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.MutableType)

