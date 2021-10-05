/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all FieldWriteAccess instructions.
 *
 * @author Michael Eichberg
 */
abstract class FieldWriteAccess extends FieldAccess {

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    final def expressionResult: NoExpression.type = NoExpression
}

/**
 * Defines an extractor to facilitate pattern matching on field write access instructions.
 *
 * @author Michael Eichberg
 */
object FieldWriteAccess {

    def unapply(fa: FieldWriteAccess): Option[(ObjectType, String, FieldType)] =
        FieldAccess.unapply(fa)
}
