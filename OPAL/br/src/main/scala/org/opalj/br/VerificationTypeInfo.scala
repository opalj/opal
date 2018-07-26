/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Part of the Java 6 stack map table attribute.
 *
 * @author Michael Eichberg
 */
sealed abstract class VerificationTypeInfo {

    def tag: Int

    def isObjectVariableInfo: Boolean = false
    def asObjectVariableInfo: ObjectVariableInfo = {
        throw new ClassCastException(s"$this cannot be cast to ObjectVariableInfo");
    }
}

case object TopVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 0
}

case object IntegerVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 1
}

case object FloatVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 2
}

case object DoubleVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 3
}

case object LongVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 4
}

case object NullVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 5
}

case object UninitializedThisVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 6
}

case class ObjectVariableInfo(clazz: ReferenceType) extends VerificationTypeInfo {
    final val tag: Int = 7

    override def isObjectVariableInfo: Boolean = true
    override def asObjectVariableInfo: this.type = this
}

case class UninitializedVariableInfo(offset: Int) extends VerificationTypeInfo {
    final val tag: Int = 8
}
