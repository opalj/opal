/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package field_mutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject

/**
 * Matches a field's `FieldMutability` property. The match is successful if the field either
 * does not have a corresponding property (in which case the fallback property will be
 * `NonFinalField`) or if the property is an instance of `NonFinalField`.
 *
 * @author Michael Eichberg
 */
class NonFinalMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (properties.forall(p ⇒ p.isInstanceOf[NonFinalField] || p.key != FieldMutability.key))
            None
        else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
