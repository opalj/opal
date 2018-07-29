/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Given a field's information a (final) attribute related to the field is build.
 *
 * @author Michael Eichberg
 */
trait FieldAttributeBuilder {

    def apply(accessFlags: Int, name: String, fieldType: FieldType): Attribute

}
