/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package field_mutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.BooleanValue
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.NonFinalField
import org.opalj.br.fpcf.properties.PrematurelyReadField

/**
 * Matches a field's `FieldMutability` property. The match is successful if the field either
 * does not have a corresponding property (in which case the fallback property will be
 * `NonFinalField`) or if the property is an instance of `NonFinalField`.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
class NonFinalMatcher extends AbstractPropertyMatcher {

    override def isRelevant(
        p:  SomeProject,
        as: Set[ObjectType],
        e:  Entity,
        a:  AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asObjectType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        if (!analyses.exists(as.contains))
            return false;

        val prematurelyRead = getValue(p, annotationType, a.elementValuePairs, "prematurelyRead").asInstanceOf[BooleanValue].value

        if (prematurelyRead) {
            val propertyStore = p.get(PropertyStoreKey)
            propertyStore(e, FieldPrematurelyRead.key) match {
                case FinalP(PrematurelyReadField) => true
                case _                            => false
            }
        } else {
            true
        }
    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (properties.forall(p => p.isInstanceOf[NonFinalField] || p.key != FieldMutability.key))
            None
        else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
