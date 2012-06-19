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
package de.tud.cs.st.bat.canonical


/**

 * @author Michael Eichberg
 */
trait ElementValue { }

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

trait SimpleElementValue extends ElementValue {
	
	//
	// ABSTRACT DEFINITIONS
	//
	
	val const_value_index : Int
}

trait ByteValue extends SimpleElementValue
object ByteValue{ val tag = 'B' }

trait CharValue extends SimpleElementValue
object CharValue{ val tag = 'C' }

trait DoubleValue extends SimpleElementValue
object DoubleValue{ val tag = 'D' }	

trait FloatValue extends SimpleElementValue
object FloatValue{ val tag = 'F' }	

trait IntValue extends SimpleElementValue
object IntValue{ val tag = 'I' }	

trait LongValue extends SimpleElementValue
object LongValue{ val tag = 'J' }	

trait ShortValue extends SimpleElementValue
object ShortValue{ val tag = 'S' }	

trait BooleanValue extends SimpleElementValue
object BooleanValue{ val tag = 'Z' }

trait StringValue extends SimpleElementValue
object StringValue{ val tag = 's' }
	
trait ClassValue extends SimpleElementValue 
object ClassValue{ val tag = 'c' }



trait StructuredElementValue extends ElementValue { }

trait EnumValue  extends StructuredElementValue {
	
	//
	// ABSTRACT DEFINITIONS
	//
	
	val type_name_index : Int
	val const_name_index : Int
}
object EnumValue{ val tag = 'e' }


trait AnnotationValue extends StructuredElementValue {
	
	//
	// ABSTRACT DEFINITIONS
	//
	
	type Annotation
	
	val annotation : Annotation
}
object AnnotationValue{ val tag = '@' }


trait ArrayValue extends StructuredElementValue {
	
	//
	// ABSTRACT DEFINITIONS
	//
			
	val values : Seq[ElementValue] // ElementValue is the supertype
} 
object ArrayValue{ val tag = '[' }
