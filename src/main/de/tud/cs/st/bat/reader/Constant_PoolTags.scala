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
package de.tud.cs.st.bat.reader

/**
 * @author Michael Eichberg
 */
object Constant_PoolTags extends Enumeration {

    /*
	 * Cf. Constant_Pool_Tags in the Java Virtual Machine Specification
	 */

    val CONSTANT_Class = Value(7, "CONSTANT_Class")
    val CONSTANT_Fieldref = Value(9, "CONSTANT_Fieldref")
    val CONSTANT_Methodref = Value(10, "CONSTANT_Methodref")
    val CONSTANT_InterfaceMethodref = Value(11, "CONSTANT_InterfaceMethodref")
    val CONSTANT_String = Value(8, "CONSTANT_String")
    val CONSTANT_Integer = Value(3, "CONSTANT_Integer")
    val CONSTANT_Float = Value(4, "CONSTANT_Float")
    val CONSTANT_Long = Value(5, "CONSTANT_Long")
    val CONSTANT_Double = Value(6, "CONSTANT_Double")
    val CONSTANT_NameAndType = Value(12, "CONSTANT_NameAndType")
    val CONSTANT_Utf8 = Value(1, "CONSTANT_Utf8")
    val CONSTANT_MethodHandle = Value(15, "CONSTANT_MethodHandle")
    val CONSTANT_MethodType = Value(16, "CONSTANT_MethodType")
    val CONSTANT_InvokeDynamic = Value(18, "CONSTANT_InvokeDynamic")
}
