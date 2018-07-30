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

}
case object ComputationalTypeInt extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress: Boolean = false
}
case object ComputationalTypeFloat extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress: Boolean = false
}
case object ComputationalTypeLong extends ComputationalType(Category2ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress: Boolean = false
}
case object ComputationalTypeDouble extends ComputationalType(Category2ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress: Boolean = false
}
case object ComputationalTypeReference
    extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress: Boolean = false
}
case object ComputationalTypeReturnAddress
    extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress: Boolean = true
}

