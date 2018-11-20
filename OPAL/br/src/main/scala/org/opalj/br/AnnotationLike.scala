/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * An annotation of a code entity.
 *
 * Annotations are associated with a class, field, method, type etc. using the respective
 * attributes.
 *
 * @author Michael Eichberg
 */
abstract class AnnotationLike {

    def annotationType: FieldType

    def elementValuePairs: ElementValuePairs

}
