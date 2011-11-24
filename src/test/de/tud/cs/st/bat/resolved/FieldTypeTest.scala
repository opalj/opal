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
package de.tud.cs.st.bat.resolved


import org.scalatest.FunSuite
import java.io.File
import java.util.zip.ZipFile
import java.util.Enumeration


/**

 * @author Michael Eichberg
 */
//@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class FieldTypeTest extends FunSuite {

	test("Byte Field Descriptor") {
		val fd = FieldDescriptor("B")
		assert (fd.fieldType.isInstanceOf[ByteType])
	}

	test("Char Field Descriptor") {
		val fd = FieldDescriptor("C")
		assert (fd.fieldType.isInstanceOf[CharType])
	}

	test("Double Field Descriptor") {
		val fd = FieldDescriptor("D")
		assert (fd.fieldType.isInstanceOf[DoubleType])
	}

	test("Float Field Descriptor") {
		val fd = FieldDescriptor("F")
		assert (fd.fieldType.isInstanceOf[FloatType])
	}

	test("Integer Field Descriptor") {
		val fd = FieldDescriptor("I")
		assert (fd.fieldType.isInstanceOf[IntegerType])
	}

	test("Long Field Descriptor") {
		val fd = FieldDescriptor("J")
		assert (fd.fieldType.isInstanceOf[LongType])
	}

	test("ObjectType Field Descriptor") {
		val fd = FieldDescriptor("Ljava/lang/Object;")
		assert (fd.fieldType.isInstanceOf[ObjectType])
		val ot : ObjectType = fd.fieldType.asInstanceOf[ObjectType]
		assert (ot.className === "java/lang/Object")
	}

	test("ArrayType (Array of References) Field Descriptor") {
		val fd = FieldDescriptor("[Ljava/lang/Object;")
		assert (fd.fieldType.isInstanceOf[ArrayType])
		val at : ArrayType = fd.fieldType.asInstanceOf[ArrayType]
		assert (at.componentType === FieldType("Ljava/lang/Object;"))
	}

	test("ArrayType (Array of Primitives) Field Descriptor") {
		val fd = FieldDescriptor("[J")
		assert (fd.fieldType.isInstanceOf[ArrayType])
		val at : ArrayType = fd.fieldType.asInstanceOf[ArrayType]
		assert (at.componentType === LongType())
	}

	test("ArrayType (Array of Array of Primitives) Field Descriptor") {
		val fd = FieldDescriptor("[[S")
		fd.fieldType match {
			case ArrayType(ArrayType(st : ShortType)) => /*OK*/
		}
	}
}
