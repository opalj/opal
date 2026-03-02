/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Text

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait ElementValue {

    /**
     * The number of bytes required to store this element value
     * in a class file.
     */
    def attribute_length: Int

    def tag: Int

    def toXHTML(implicit cp: Constant_Pool): Node

}

trait BaseElementValue extends ElementValue {

    override final def attribute_length: Int = 1 + 2

    def const_value_index: Constant_Pool_Index

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value">{cp(const_value_index).asInstructionParameter}</div>
    }

}

case class ByteValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = ByteValue.tag
}
object ByteValue { final val tag: Int = 'B' }

case class CharValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = CharValue.tag
}
object CharValue { final val tag: Int = 'C' }

case class DoubleValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = DoubleValue.tag
}
object DoubleValue { final val tag: Int = 'D' }

case class FloatValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = FloatValue.tag
}
object FloatValue { final val tag: Int = 'F' }

case class IntValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = IntValue.tag
}
object IntValue { final val tag: Int = 'I' }

case class LongValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = LongValue.tag
}
object LongValue { final val tag: Int = 'J' }

case class ShortValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = ShortValue.tag
}
object ShortValue { final val tag: Int = 'S' }

case class BooleanValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    override final def tag: Int = BooleanValue.tag
}
object BooleanValue { final val tag: Int = 'Z' }

case class StringValue(const_value_index: Constant_Pool_Index) extends ElementValue {

    override final def attribute_length: Int = 1 + 2

    override final def tag: Int = StringValue.tag

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value">"{cp(const_value_index).toString}"</div>
    }
}
object StringValue { final val tag: Int = 's' }

case class ClassValue(class_info_index: Constant_Pool_Index) extends ElementValue {

    override final def attribute_length: Int = 1 + 2

    override final def tag: Int = ClassValue.tag

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value type">{returnTypeAsJavaType(class_info_index).asSpan("")}.class</div>
    }

}
object ClassValue { final val tag: Int = 'c' }

trait StructuredElementValue extends ElementValue

case class EnumValue(
    type_name_index:  Constant_Pool_Index,
    const_name_index: Constant_Pool_Index
) extends StructuredElementValue {
    override final def attribute_length: Int = 1 + 2 + 2

    override final def tag: Int = EnumValue.tag

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val et = parseFieldType(type_name_index).asSpan("")
        val ec = cp(const_name_index).toString

        <div class="constant_value">{et}.<span class="field_name">{ec}</span></div>
    }

}
object EnumValue { final val tag: Int = 'e' }

case class AnnotationValue(annotation: Annotation) extends StructuredElementValue {

    override final def attribute_length: Int = 1 + annotation.attribute_length

    override final def tag: Int = AnnotationValue.tag

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value">{annotation.toXHTML}</div>
    }

}
object AnnotationValue { final val tag: Int = '@' }

case class ArrayValue(values: Seq[ElementValue]) extends StructuredElementValue {

    override final def attribute_length: Int = {
        1 + values.foldLeft(2 /*num_values*/ )((c, n) => c + n.attribute_length)
    }

    override final def tag: Int = ArrayValue.tag

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val valueNodes =
            if (values.nonEmpty)
                this.values.map(v => Seq(v.toXHTML)).reduce((c, n) => c ++: (Text(", ") +: n))
            else
                NodeSeq.Empty
        <div class="constant_value">[{valueNodes}]</div>
    }

}
object ArrayValue { final val tag: Int = '[' }
