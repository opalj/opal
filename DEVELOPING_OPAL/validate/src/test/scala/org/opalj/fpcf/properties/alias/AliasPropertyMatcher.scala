/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package alias

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.Alias

/**
 * Base class for matching the assigned alias property of an entity with the expected alias property.
 * @param property The expected alias property
 */
abstract class AliasPropertyMatcher(val property: Alias) extends AbstractPropertyMatcher {

    override def isRelevant(
        p:      Project[_],
        as:     Set[ObjectType],
        entity: Any,
        a:      AnnotationLike
    ): Boolean = {
        val analysesElementValues = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues map {
            _.asClassValue.value.asObjectType
        }

        analyses.exists(as.contains)
    }

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {

        if (properties.isEmpty) {
            return Some("No Properties assigned to entity")
        }
        if (properties.count(_.isInstanceOf[Alias]) > 1) {
            return Some("Multiple alias properties found")
        }
        if (!properties.exists(p => p == property)) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }

    }
}

/**
 * Matches if the entity has the [[org.opalj.br.fpcf.properties.NoAlias]] property.
 */
class NoAliasMatcher extends AliasPropertyMatcher(org.opalj.br.fpcf.properties.NoAlias)

/**
 * Matches if the entity has the [[org.opalj.br.fpcf.properties.MayAlias]] property.
 */
class MayAliasMatcher extends AliasPropertyMatcher(org.opalj.br.fpcf.properties.MayAlias)

/**
 * Matches if the entity has the [[org.opalj.br.fpcf.properties.MustAlias]] property.
 */
class MustAliasMatcher extends AliasPropertyMatcher(org.opalj.br.fpcf.properties.MustAlias)
