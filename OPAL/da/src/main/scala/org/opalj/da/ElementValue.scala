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

    final override def attribute_length: Int = 1 + 2

    def const_value_index: Constant_Pool_Index

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value">{ cp(const_value_index).asInstructionParameter }</div>
    }

}

case class ByteValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = ByteValue.tag.toInt
}
object ByteValue { final val tag: Int = 'B' }

case class CharValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = CharValue.tag.toInt
}
object CharValue { final val tag: Int = 'C' }

case class DoubleValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = DoubleValue.tag.toInt
}
object DoubleValue { final val tag: Int = 'D' }

case class FloatValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = FloatValue.tag.toInt
}
object FloatValue { final val tag: Int = 'F' }

case class IntValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = IntValue.tag.toInt
}
object IntValue { final val tag: Int = 'I' }

case class LongValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = LongValue.tag.toInt
}
object LongValue { final val tag: Int = 'J' }

case class ShortValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = ShortValue.tag.toInt
}
object ShortValue { final val tag: Int = 'S' }

case class BooleanValue(const_value_index: Constant_Pool_Index) extends BaseElementValue {
    final override def tag: Int = BooleanValue.tag.toInt
}
object BooleanValue { final val tag: Int = 'Z' }

case class StringValue(const_value_index: Constant_Pool_Index) extends ElementValue {

    final override def attribute_length: Int = 1 + 2

    final override def tag: Int = StringValue.tag.toInt

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value">"{ cp(const_value_index).toString }"</div>
    }
}
object StringValue { final val tag: Int = 's' }

case class ClassValue(class_info_index: Constant_Pool_Index) extends ElementValue {

    final override def attribute_length: Int = 1 + 2

    final override def tag: Int = ClassValue.tag.toInt

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value type">{ returnTypeAsJavaType(class_info_index).asSpan("") }.class</div>
    }

}
object ClassValue { final val tag: Int = 'c' }

trait StructuredElementValue extends ElementValue

case class EnumValue(
        type_name_index:  Constant_Pool_Index,
        const_name_index: Constant_Pool_Index
) extends StructuredElementValue {
    final override def attribute_length: Int = 1 + 2 + 2

    final override def tag: Int = EnumValue.tag.toInt

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val et = parseFieldType(type_name_index).asSpan("")
        val ec = cp(const_name_index).toString

        <div class="constant_value">{ et }.<span class="field_name">{ ec }</span></div>
    }

}
object EnumValue { final val tag: Int = 'e' }

case class AnnotationValue(annotation: Annotation) extends StructuredElementValue {

    final override def attribute_length: Int = 1 + annotation.attribute_length

    final override def tag: Int = AnnotationValue.tag.toInt

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="constant_value">{ annotation.toXHTML }</div>
    }

}
object AnnotationValue { final val tag: Int = '@' }

case class ArrayValue(values: Seq[ElementValue]) extends StructuredElementValue {

    final override def attribute_length: Int = {
        1 + values.foldLeft(2 /*num_values*/ )((c, n) => c + n.attribute_length)
    }

    final override def tag: Int = ArrayValue.tag.toInt

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val valueNodes =
            if (values.nonEmpty)
                this.values.map(v => Seq(v.toXHTML)).reduce((c, n) => c ++: (Text(", ") +: n))
            else
                NodeSeq.Empty
        <div class="constant_value">[{ valueNodes }]</div>
    }

}
object ArrayValue { final val tag: Int = '[' }
