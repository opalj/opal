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
package de.tud.cs.st.bat.native


/**
 * @author Michael Eichberg
 */
trait Constant_Pool_Entry {

	//
	// ABSTRACT DEFINITIONS
	//
	
	def Constant_Type_Value : Constant_Pool_Entry.Value
  
}

// TODO Move to the generic package because the following information is required, when reading a class file
object Constant_Pool_Entry extends Enumeration {

	val CONSTANT_Class_ID					= 7
	val CONSTANT_Fieldref_ID 					= 9
	val CONSTANT_Methodref_ID 					= 10
	val CONSTANT_InterfaceMethodref_ID 		= 11
	val CONSTANT_String_ID 						= 8
	val CONSTANT_Integer_ID 					= 3
	val CONSTANT_Float_ID 						= 4
	val CONSTANT_Long_ID 						= 5
	val CONSTANT_Double_ID 						= 6
	val CONSTANT_NameAndType_ID 				= 12
	val CONSTANT_Utf8_ID 						= 1
	// used by Java 7
	val CONSTANT_MethodHandle_ID				= 15
	val CONSTANT_MethodType_ID					= 16
	val CONSTANT_InvokeDynamic_ID				= 18

	
	/*
	 * Cf. Constant_Pool_Tags in the Java Virtual Machine Specification
	 */
	
	val CONSTANT_Class 						= Value(7,"CONSTANT_Class")
	val CONSTANT_Fieldref 					= Value(9,"CONSTANT_Fieldref")
	val CONSTANT_Methodref 					= Value(10,"CONSTANT_Methodref")
	val CONSTANT_InterfaceMethodref 		= Value(11,"CONSTANT_InterfaceMethodref")
	val CONSTANT_String 						= Value(8,"CONSTANT_String")
	val CONSTANT_Integer 					= Value(3,"CONSTANT_Integer")
	val CONSTANT_Float 						= Value(4,"CONSTANT_Float")
	val CONSTANT_Long 						= Value(5,"CONSTANT_Long")
	val CONSTANT_Double 						= Value(6,"CONSTANT_Double")
	val CONSTANT_NameAndType 				= Value(12,"CONSTANT_NameAndType")
	val CONSTANT_Utf8 						= Value(1,"CONSTANT_Utf8")
	// used by Java 7
	val CONSTANT_MethodHandle				= Value(15,"CONSTANT_MethodHandle")
	val CONSTANT_MethodType					= Value(16,"CONSTANT_MethodType")
	val CONSTANT_InvokeDynamic				= Value(18,"CONSTANT_InvokeDynamic")
	
	
	
}