/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Instructions that access a class' field.
 *
 * @author Michael Eichberg
 */
abstract class FieldAccess extends Instruction with ConstantLengthInstruction with NoLabels {

    def declaringClass: ObjectType

    def name: String

    def fieldType: FieldType

    def asVirtualField: VirtualField = VirtualField(declaringClass, name, fieldType)

    final def length: Int = 3

    final def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }

    final def readsLocal: Boolean = false

    final def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    final def writesLocal: Boolean = false

    final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final override def toString(currentPC: Int): String = toString()
}

/**
 * Defines an extractor to facilitate pattern matching on field access instructions.
 */
object FieldAccess {

    val jvmExceptions = List(ObjectType.NullPointerException)

    def unapply(fa: FieldAccess): Option[(ObjectType, String, FieldType)] = {
        Some((fa.declaringClass, fa.name, fa.fieldType))
    }
}
