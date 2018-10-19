/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Represents a class or interface.
 *
 * `ConstantClass` is, e.g., used by `anewarray` and `multianewarray` instructions.
 * A `ConstantClass` value is never a `Field` value. I.e., it is never used to
 * set the value of a static final field.
 */
final case class ConstantClass(value: ReferenceType) extends ConstantValue[ReferenceType] {

    override def valueToString = value.toJava

    final def toJava = valueToString+".class"

    override def runtimeValueType = ObjectType.Class

    final override def toReferenceType: ReferenceType = value
}
