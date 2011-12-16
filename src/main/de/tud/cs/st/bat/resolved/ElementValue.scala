/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package resolved

/**
 * An annotation's value.
 *
 * @author Michael Eichberg
 */
sealed trait ElementValue extends Attribute {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T

    def valueToXML: scala.xml.Node

    /**
     * XML representation of this element value as if it defines an annotation
     * default attribute.
     */
    final def toXML = <annotation_default>{ valueToXML }</annotation_default>

    final def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A], declaringEntityKey: A): List[F] =
        factory.Fact("annotation_default", declaringEntityKey, valueToProlog(factory)) :: Nil
}

case class ByteValue(value: Byte) extends ElementValue {

    def valueToXML = <byte value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T =
        factory.Term("byte", factory.IntegerAtom(value))

}

case class CharValue(value: Char) extends ElementValue {

    def valueToXML = <char value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "char",
            factory.IntegerAtom(value)
        )
    }
}

case class DoubleValue(value: Double) extends ElementValue {

    def valueToXML = <double value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "double",
            factory.FloatAtom(value)
        )
    }
}

case class FloatValue(value: Float) extends ElementValue {

    def valueToXML = <float value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "float",
            factory.FloatAtom(value)
        )
    }
}

case class IntValue(value: Int) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <int value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "int",
            factory.IntegerAtom(value)
        )
    }
}

case class LongValue(value: Long) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <long value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "long",
            factory.IntegerAtom(value)
        )
    }
}

case class ShortValue(value: Short) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <short value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "short",
            factory.IntegerAtom(value)
        )
    }
}

case class BooleanValue(value: Boolean) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <boolean value={ value.toString }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "boolean",
            if (value) factory.YesAtom else factory.NoAtom
        )
    }
}

case class StringValue(value: String) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <string>{ value.toString }</string>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "string",
            factory.TextAtom(value)
        )
    }
}

case class ClassValue(value: Type) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <class type={ value.toJava }/>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T =
        factory.Term("class", value.toProlog(factory))

}

case class EnumValue(enumType: ObjectType, val constName: String) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <enum type={ enumType.className }>{ constName }</enum>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "enum",
            enumType.toProlog(factory),
            factory.TextAtom(constName)
        )
    }
}

case class ArrayValue(values: IndexedSeq[ElementValue]) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = <array>{ for (value ← values) yield value.toXML }</array>

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "array",
            factory.Terms(values, (_: ElementValue).valueToProlog(factory))
        )
    }
}

case class AnnotationValue(annotation: Annotation) extends ElementValue {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def valueToXML = annotation.toXML

    def valueToProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        annotation.toProlog(factory)
    }
}


