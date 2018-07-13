/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package properties
package thrown_exceptions

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property

/**
 * Matches a methods's `ThrownExceptions` property.
 *
 * @author Andreas Muttscheller
 */
class DoesNotThrowExceptionMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType
        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "requires").asArrayValue.values
        val requiredAnalysis = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)

        // Succeed either if the required analysis did NOT run, or the property is correct
        val isPropertyValid = !requiredAnalysis.exists(as.contains) ||
            properties.forall { p ⇒
                p.key != ThrownExceptions.key || // If we got another key, ignore
                    (p.isInstanceOf[ThrownExceptions] &&
                        p.asInstanceOf[ThrownExceptions].throwsNoExceptions)
            }
        if (isPropertyValid)
            None
        else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
