/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_definition

import org.opalj.br.analyses.Project
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.Property

/**
 * Matches local variable's `StringConstancy` property. The match is successful if the
 * variable has a constancy level that matches its usage and the expected values are present.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionMatcher extends AbstractPropertyMatcher {

    /**
     * @inheritdoc
     */
    override def validateProperty(
                                     p:          Project[_],
                                     as:         Set[ObjectType],
                                     entity:     Any,
                                     a:          AnnotationLike,
                                     properties: Traversable[Property]
                                 ): Option[String] = {
        None
        // TODO: Implement the matcher
    }

}
