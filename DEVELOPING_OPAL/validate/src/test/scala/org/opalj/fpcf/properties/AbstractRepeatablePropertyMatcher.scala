/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.AnnotationLike
import org.opalj.br.ClassType
import org.opalj.br.analyses.Project

/**
 * Basic property matcher for repeatable annotations.
 *
 * @author Robin Körkemeier
 */
abstract class AbstractRepeatablePropertyMatcher extends AbstractPropertyMatcher {
    /**
     * Type for identifying the single annotation
     */
    val singleAnnotationType: ClassType
    /**
     * Type for identifying the container annotation
     */
    val containerAnnotationType: ClassType

    /**
     * The name of the field of the container annotation that holds the single annotations
     */
    val containerAnnotationFieldName: String = "value"

    override def validateProperty(
        p:          Project[?],
        as:         Set[ClassType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (a.annotationType == singleAnnotationType) {
            validateSingleProperty(p, as, entity, a, properties)
        } else if (a.annotationType == containerAnnotationType) {
            val subAnnotations =
                getValue(p, containerAnnotationType, a.elementValuePairs, containerAnnotationFieldName)
                    .asArrayValue.values.map { a => a.asAnnotationValue.annotation }

            val errors = subAnnotations
                .map { a => validateSingleProperty(p, as, entity, a, properties) }
                .filter { result => result.isDefined }
                .map { result => result.get }

            if (errors.nonEmpty) {
                Some(errors.mkString(" "))
            } else {
                None
            }
        } else {
            Some(s"Invalid annotation '${a.annotationType}' for ${this.getClass.getName}!")
        }
    }

    /**
     * Test if the computed properties are matched by this matcher. Called for each single annotation of a container
     * annotation once.
     */
    def validateSingleProperty(
        p:          Project[?],
        as:         Set[ClassType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String]
}
