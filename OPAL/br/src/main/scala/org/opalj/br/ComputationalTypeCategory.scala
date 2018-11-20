/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The computational type category of a value on the operand stack.
 *
 * (cf. JVM Spec. 2.11.1 Types and the Java Virtual Machine).
 *
 * @author Michael Eichberg
 */
sealed abstract class ComputationalTypeCategory {
    /**
     * The size of operands of the values of this category
     */
    val operandSize: Int

    /**
     * Identifies the computational type category.
     */
    val id: Int
}
case object Category1ComputationalTypeCategory extends ComputationalTypeCategory {
    final val operandSize = 1
    final val id /*: Byte*/ = 1
}
case object Category2ComputationalTypeCategory extends ComputationalTypeCategory {
    final val operandSize = 2
    final val id /*: Byte*/ = 2
}
