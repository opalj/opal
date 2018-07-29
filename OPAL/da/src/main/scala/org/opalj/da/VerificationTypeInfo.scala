/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
trait VerificationTypeInfo {

    /**
     * The number of bytes required to store the VerificationTypeInfo
     * information in a class file.
     */
    def attribute_length: Int

    def tag: Int

    def toXHTML(implicit cp: Constant_Pool): Node

}

object VerificationTypeInfo {
    final val ITEM_Top = 0
    final val ITEM_Integer = 1
    final val ITEM_Float = 2
    final val ITEM_Long = 4
    final val ITEM_Double = 3
    final val ITEM_Null = 5
    final val ITEM_UninitializedThis = 6
    final val ITEM_Object = 7
    final val ITEM_Unitialized = 8
}

case object TopVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_Top

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="verification_type">&lt;TOP&gt;</span>
    }
}

case object IntegerVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_Integer

    def toXHTML(implicit cp: Constant_Pool): Node = <span class="verification_type">int</span>

}

case object FloatVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_Float

    def toXHTML(implicit cp: Constant_Pool): Node = <span class="verification_type">float</span>

}

case object LongVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_Long

    def toXHTML(implicit cp: Constant_Pool): Node = <span class="verification_type">long</span>

}

case object DoubleVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_Double

    def toXHTML(implicit cp: Constant_Pool): Node = <span class="verification_type">double</span>

}

case object NullVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_Null

    def toXHTML(implicit cp: Constant_Pool): Node = <span class="verification_type">null</span>

}

case object UninitializedThisVariableInfo extends VerificationTypeInfo {

    final override def attribute_length: Int = 1

    def tag: Int = VerificationTypeInfo.ITEM_UninitializedThis

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="verification_type">&lt;UninitializedThis&gt;</span>
    }
}

case class ObjectVariableInfo(cpool_index: Int) extends VerificationTypeInfo {

    final override def attribute_length: Int = 1 + 2

    def tag: Int = VerificationTypeInfo.ITEM_Object

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val referenceType = asJavaReferenceType(cpool_index)
        referenceType.asSpan("verification_type")
    }
}

case class UninitializedVariableInfo(val offset: Int) extends VerificationTypeInfo {

    final override def attribute_length: Int = 1 + 2

    def tag: Int = VerificationTypeInfo.ITEM_Unitialized

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="verification_type">&lt;Uninitialized({ offset })&gt;</span>
    }
}
