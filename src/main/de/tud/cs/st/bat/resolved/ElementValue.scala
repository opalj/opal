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

import scala.xml.Node


/**
 * The values of an annotation.
 *
 * <p>
 * 	From the specification:
 * 	<pre>
 * element_value {
 * u1 tag;
 * union {
 * u2   const_value_index;
 *
 * {
 * u2 type_name_index;
 * u2 const_name_index;
 * } enum_const_value;
 *
 * u2 class_info_index;
 *
 * annotation annotation_value;
 *
 * {
 * u2    num_values;
 * element_value values[num_values];
 * } array_value;
 * } value;
 * }
 * 	</pre>
 * </p>
 *
 * @author Michael Eichberg
 */
sealed trait ElementValue {

    def toXML: Node

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T

}

case class ByteValue(val value: Byte) extends ElementValue {
    
    def toXML = <byte value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T =
        factory.Term("byte", factory.IntegerAtom(value))

}

case class CharValue(val value: Char) extends ElementValue {
    def toXML = <char value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "char",
            factory.IntegerAtom(value)
        )
    }
}

case class DoubleValue(val value: Double) extends ElementValue {
    def toXML = <double value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "double",
            factory.FloatAtom(value)
        )
    }
}

case class FloatValue(val value: Float) extends ElementValue {
    def toXML = <float value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "float",
            factory.FloatAtom(value)
        )
    }
}

case class IntValue(val value: Int) extends ElementValue {
    def toXML = <int value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "int",
            factory.IntegerAtom(value)
        )
    }
}

case class LongValue(val value: Long) extends ElementValue {
    def toXML = <long value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "long",
            factory.IntegerAtom(value)
        )
    }
}

case class ShortValue(val value: Short) extends ElementValue {
    def toXML = <short value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "short",
            factory.IntegerAtom(value)
        )
    }
}

case class BooleanValue(val value: Boolean) extends ElementValue {
    def toXML = <boolean value={ value.toString }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "boolean",
            if (value) factory.YesAtom else factory.NoAtom
        )
    }
}

case class StringValue(val value: String) extends ElementValue {
    def toXML = <string>{ value.toString }</string>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "string",
            factory.TextAtom(value)
        )
    }
}

case class ClassValue(val value: ReturnType) extends ElementValue {
    def toXML = <class type={ value.toJava }/>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "class",
            value.toProlog(factory) // TODO doesn't this lead to awkward class(class(...,...)) structures?
        )
    }
}

case class EnumValue(val enumType: ObjectType, val constName: String) extends ElementValue {
    def toXML = <enum type={ enumType.className }>{ constName }</enum>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "enum",
            enumType.toProlog(factory),
            factory.TextAtom(constName)
        )
    }
}

case class ArrayValue(val values: IndexedSeq[ElementValue]) extends ElementValue {
    def toXML = <array>{ for (value ← values) yield value.toXML }</array>

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        factory.Term(
            "array",
            factory.Terms(values, (_: ElementValue).toProlog(factory))
        )
    }
}

case class AnnotationValue(val annotation: Annotation) extends ElementValue {
    def toXML = annotation.toXML

    def toProlog[F, T, A <: T](factory: PrologTermFactory[F, T, A]): T = {
        annotation.toProlog(factory)
    }
}


