/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The computational type of a value on the operand stack.
 *
 * (cf. JVM Spec. 2.11.1 Types and the Java Virtual Machine).
 */
sealed abstract class ComputationalType(val category: ComputationalTypeCategory) {

    def categoryId: Int = category.id

    def operandSize: Int = category.operandSize

    def isComputationalTypeReturnAddress: Boolean

    def isCategory2: Boolean

}
case object ComputationalTypeInt extends ComputationalType(Category1ComputationalTypeCategory) {
    final override def isComputationalTypeReturnAddress: Boolean = false
    final override def isCategory2: Boolean = false
}
case object ComputationalTypeFloat extends ComputationalType(Category1ComputationalTypeCategory) {
    final override def isComputationalTypeReturnAddress: Boolean = false
    final override def isCategory2: Boolean = false
}
case object ComputationalTypeLong extends ComputationalType(Category2ComputationalTypeCategory) {
    final override def isComputationalTypeReturnAddress: Boolean = false
    final override def isCategory2: Boolean = true
}
case object ComputationalTypeDouble extends ComputationalType(Category2ComputationalTypeCategory) {
    final override def isComputationalTypeReturnAddress: Boolean = false
    final override def isCategory2: Boolean = true
}
case object ComputationalTypeReference
    extends ComputationalType(Category1ComputationalTypeCategory) {
    final override def isComputationalTypeReturnAddress: Boolean = false
    final override def isCategory2: Boolean = false
}
case object ComputationalTypeReturnAddress
    extends ComputationalType(Category1ComputationalTypeCategory) {
    final override def isComputationalTypeReturnAddress: Boolean = true
    final override def isCategory2: Boolean = false
}

