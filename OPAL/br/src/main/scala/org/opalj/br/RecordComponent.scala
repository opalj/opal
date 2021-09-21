/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The description of a record component (Java 16).
 *
 * @author Dominik Helm
 */
case class RecordComponent(
        name:          String,
        componentType: FieldType,
        attributes:    Attributes
)
