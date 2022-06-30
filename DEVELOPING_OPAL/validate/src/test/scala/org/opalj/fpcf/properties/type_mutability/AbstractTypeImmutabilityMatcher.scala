/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package type_mutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.TypeImmutability

class AbstractTypeImmutabilityMatcher(
        val property: TypeImmutability
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

class ImmutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.ImmutableType)
class ImmutableContainerTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.ImmutableContainerType)
class MutableTypeMatcher
    extends AbstractTypeImmutabilityMatcher(org.opalj.br.fpcf.properties.MutableType)
