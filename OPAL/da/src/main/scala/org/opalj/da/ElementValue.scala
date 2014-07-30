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

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait ElementValue {}

/* TABLE: BaseType characters (JVM Spec. Table 4.2)
	BaseType 	Character Type 	Interpretation 
	B 				byte 					signed byte 
	C 				char 					Unicode character 
	D 				double 				double-precision ﬂoating-point value 
	F 				float 				single-precision ﬂoating-point value 
	I 				int 					integer 
	J 				long 					long integer 
	S 				short 				signed short 
	Z 				boolean 				true orfalse 
	[ 				reference 			one array dimension
*/

case class ByteValue(const_value_index: Int) extends ElementValue
object ByteValue { val tag = 'B' }

case class CharValue(const_value_index: Int) extends ElementValue
object CharValue { val tag = 'C' }

case class DoubleValue(const_value_index: Int) extends ElementValue
object DoubleValue { val tag = 'D' }

case class FloatValue(const_value_index: Int) extends ElementValue
object FloatValue { val tag = 'F' }

case class IntValue(const_value_index: Int) extends ElementValue
object IntValue { val tag = 'I' }

case class LongValue(const_value_index: Int) extends ElementValue
object LongValue { val tag = 'J' }

case class ShortValue(const_value_index: Int) extends ElementValue
object ShortValue { val tag = 'S' }

case class BooleanValue(const_value_index: Int) extends ElementValue
object BooleanValue { val tag = 'Z' }

case class StringValue(const_value_index: Int) extends ElementValue
object StringValue { val tag = 's' }

case class ClassValue(const_value_index: Int) extends ElementValue
object ClassValue { val tag = 'c' }

trait StructuredElementValue extends ElementValue {}

case class EnumValue(
    type_name_index: Int,
    const_name_index: Int) extends StructuredElementValue
object EnumValue { val tag = 'e' }

case class AnnotationValue(val annotation: Annotation) extends StructuredElementValue
object AnnotationValue { val tag = '@' }

case class ArrayValue(val values: Seq[ElementValue]) extends StructuredElementValue
object ArrayValue { val tag = '[' }
