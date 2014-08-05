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
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait ElementValue {

    def toXHTML(implicit cp: Constant_Pool): Node

}

trait BaseElementValue extends ElementValue {
    def const_value_index: Int
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="constant_value">{ cp(const_value_index).toLDCString }</span>
    }
}

case class ByteValue(const_value_index: Int) extends BaseElementValue
object ByteValue { val tag = 'B' }

case class CharValue(const_value_index: Int) extends BaseElementValue
object CharValue { val tag = 'C' }

case class DoubleValue(const_value_index: Int) extends BaseElementValue
object DoubleValue { val tag = 'D' }

case class FloatValue(const_value_index: Int) extends BaseElementValue
object FloatValue { val tag = 'F' }

case class IntValue(const_value_index: Int) extends BaseElementValue
object IntValue { val tag = 'I' }

case class LongValue(const_value_index: Int) extends BaseElementValue
object LongValue { val tag = 'J' }

case class ShortValue(const_value_index: Int) extends BaseElementValue
object ShortValue { val tag = 'S' }

case class BooleanValue(const_value_index: Int) extends BaseElementValue
object BooleanValue { val tag = 'Z' }

case class StringValue(const_value_index: Int) extends ElementValue {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="constant_value">"{ cp(const_value_index).toString }"</span>
    }
}

object StringValue { val tag = 's' }

case class ClassValue(const_value_index: Int) extends ElementValue {
    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="constant_value type">{ parseReturnType(const_value_index) }.class</span>
    }
}
object ClassValue { val tag = 'c' }

trait StructuredElementValue extends ElementValue {}

case class EnumValue(
        type_name_index: Int,
        const_name_index: Int) extends StructuredElementValue {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val et = parseFieldType(type_name_index)
        val ec = cp(const_name_index).toString

        <span class="constant_value"><span class="type">{ et }</span>.<span class="field_name">{ ec }</span></span>
    }
}
object EnumValue { val tag = 'e' }

case class AnnotationValue(val annotation: Annotation) extends StructuredElementValue {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <span class="constant_value">{ annotation.toXHTML }</span>
    }
}
object AnnotationValue { val tag = '@' }

case class ArrayValue(val values: Seq[ElementValue]) extends StructuredElementValue {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val values = this.values.map(v ⇒ { v.toXHTML })
        <span class="constant_value">[{ values }]</span>
    }
}
object ArrayValue { val tag = '[' }
