/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package compile_time_constancy

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject

/**
 * Base trait for matchers that match a field's `CompileTimeConstancy` property.
 *
 * @author Dominik Helm
 */
sealed abstract class CompileTimeConstancyMatcher(val property: CompileTimeConstancy)
    extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (!properties.exists(_ match {
            case `property` ⇒ true
            case _          ⇒ false
        })) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

/**
 * Matches a field's `CompileTimeConstancy` property. The match is successful if the field has the
 * property [[org.opalj.fpcf.properties.CompileTimeConstantField]].
 */
class CompileTimeConstantMatcher extends CompileTimeConstancyMatcher(CompileTimeConstantField)

/**
 * Matches a field's `CompileTimeConstancy` property. The match is successful if the field has the
 * property [[org.opalj.fpcf.properties.CompileTimeVaryingField]].
 */
class CompileTimeVaryingMatcher extends CompileTimeConstancyMatcher(CompileTimeVaryingField)

