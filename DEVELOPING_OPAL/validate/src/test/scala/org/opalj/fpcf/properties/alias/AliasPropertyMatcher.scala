/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package alias

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.MayAlias
import org.opalj.br.fpcf.properties.alias.MustAlias
import org.opalj.br.fpcf.properties.alias.NoAlias

/**
 * Base class for matching the assigned alias property of an entity with the expected alias property.
 *
 * @param property The expected alias property
 */
abstract class AliasPropertyMatcher(val property: Alias) extends AbstractPropertyMatcher {

    override def isRelevant(
        p:      Project[_],
        as:     Set[ObjectType],
        entity: Any,
        a:      AnnotationLike
    ): Boolean = {
        val analysesElementValues =
            getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "analyses").asArrayValue.values
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
        if (!properties.exists(p => p == property)) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

/**
 * Matches if the entity has the [[NoAlias]] property.
 */
class NoAliasMatcher extends AliasPropertyMatcher(NoAlias)

/**
 * Matches if the entity has the [[MayAlias]] property.
 */
class MayAliasMatcher extends AliasPropertyMatcher(MayAlias)

/**
 * Matches if the entity has the [[MustAlias]] property.
 */
class MustAliasMatcher extends AliasPropertyMatcher(MustAlias)
