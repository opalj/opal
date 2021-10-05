/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Common superclass of all field read instructions.
 *
 * @author Michael Eichberg
 */
abstract class FieldReadAccess extends FieldAccess {

    final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def expressionResult: Stack.type = Stack
}

/**
 * Defines an extractor to facilitate pattern matching on field read access instructions.
 *
 * @author Michael Eichberg
 */
object FieldReadAccess {

    def unapply(fa: FieldReadAccess): Option[(ObjectType, String, FieldType)] =
        FieldAccess.unapply(fa)
}
