/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * Encapsulates basic type information.
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeInfo {

    def asJava: String

    /**
     * `true` if the underlying type (in case of an array the element type) is a base type/
     * primitive type; `false` in all other cases except if the "type" is void. In that case
     * an exception is thrown.
     */
    def elementTypeIsBaseType: Boolean

    def isVoid: Boolean

    def asSpan(baseClass: String): Node
}

object TypeInfo {
    def unapply(ti: TypeInfo): Some[(String, Boolean)] = {
        Some((ti.asJava, ti.elementTypeIsBaseType))
    }
}

case object VoidTypeInfo extends TypeInfo {

    override def asJava: String = "void"

    override def elementTypeIsBaseType: Boolean = throw new UnsupportedOperationException

    override def isVoid: Boolean = true

    override def asSpan(baseClass: String): Node = <span class={ s"$baseClass void_type" }>void</span>

}

sealed abstract class FieldTypeInfo extends TypeInfo {
    final override def isVoid: Boolean = false
}

sealed abstract class PrimitiveTypeInfo protected (val asJava: String) extends FieldTypeInfo {

    final override def elementTypeIsBaseType: Boolean = true

    override def asSpan(baseClass: String): Node = {
        <span class={ s"$baseClass primitive_type" }>{ asJava }</span>
    }
}

case object BooleanTypeInfo extends PrimitiveTypeInfo("boolean")
case object ByteTypeInfo extends PrimitiveTypeInfo("byte")
case object CharTypeInfo extends PrimitiveTypeInfo("char")
case object ShortTypeInfo extends PrimitiveTypeInfo("short")
case object IntTypeInfo extends PrimitiveTypeInfo("int")
case object LongTypeInfo extends PrimitiveTypeInfo("long")
case object FloatTypeInfo extends PrimitiveTypeInfo("float")
case object DoubleTypeInfo extends PrimitiveTypeInfo("double")

case class ObjectTypeInfo(asJava: String) extends FieldTypeInfo {

    assert(asJava.indexOf('/') == -1)

    def asJVMType: String = asJava.replace('.', '/')

    final override def elementTypeIsBaseType: Boolean = false

    override def asSpan(baseClass: String): Node = {
        <span class={ s"$baseClass object_type" }>{ asJava }</span>
    }
}

case class ArrayTypeInfo(
        elementTypeAsJava:     String,
        dimensions:            Int,
        elementTypeIsBaseType: Boolean
) extends FieldTypeInfo {

    assert(dimensions > 0)

    def asJava: String = elementTypeAsJava + ("[]" * dimensions)

    override def asSpan(baseClass: String): Node = {
        if (elementTypeIsBaseType)
            <span class={ s"$baseClass array primitive_type" }>{ asJava }</span>
        else
            <span class={ s"$baseClass array object_type" }>{ asJava }</span>
    }
}
