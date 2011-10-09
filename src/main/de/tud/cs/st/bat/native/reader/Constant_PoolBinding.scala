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
package reader

import de.tud.cs.st.bat.generic.reader.Constant_PoolReader


/**
 * Representation of the constant pool as specified by the JVM Spec. 
 * (This representation does not provide any abstraction.)
 *
 * @author Michael Eichberg
 */
trait Constant_PoolBinding extends Constant_PoolReader /*with Constant_PoolAbstractions*/ {



	// ______________________________________________________________________________________________
	//
	// REPRESENTATION OF THE CONSTANT POOL
	// ______________________________________________________________________________________________
	//

	type Constant_Pool_Entry = de.tud.cs.st.bat.native.Constant_Pool_Entry
	val Constant_Pool_EntryManifest : ClassManifest[Constant_Pool_Entry] = implicitly


	case class CONSTANT_Class_info(val name_index:Int) extends de.tud.cs.st.bat.native.CONSTANT_Class_info 
	def CONSTANT_Class_info(i : Int) : CONSTANT_Class_info = new CONSTANT_Class_info(i)


	case class CONSTANT_Double_info(val value:Double) extends de.tud.cs.st.bat.native.CONSTANT_Double_info 
	def CONSTANT_Double_info(d : Double) : CONSTANT_Double_info = new CONSTANT_Double_info(d)


	case class CONSTANT_Float_info(val value:Float) extends de.tud.cs.st.bat.native.CONSTANT_Float_info 
	def CONSTANT_Float_info(f : Float) : CONSTANT_Float_info = new CONSTANT_Float_info(f)


	case class CONSTANT_Integer_info(val value:Int) extends de.tud.cs.st.bat.native.CONSTANT_Integer_info 
	def CONSTANT_Integer_info(i : Int) : CONSTANT_Integer_info = new CONSTANT_Integer_info(i) 


	case class CONSTANT_Long_info(val value:Long) extends de.tud.cs.st.bat.native.CONSTANT_Long_info 		
	def CONSTANT_Long_info(l : Long) : CONSTANT_Long_info = new CONSTANT_Long_info(l)


	case class CONSTANT_Utf8_info(val value:String) extends de.tud.cs.st.bat.native.CONSTANT_Utf8_info 		
	def CONSTANT_Utf8_info(s : String) : CONSTANT_Utf8_info = new CONSTANT_Utf8_info(s) 


	case class CONSTANT_String_info(val string_index:Int) extends de.tud.cs.st.bat.native.CONSTANT_String_info 	
	def CONSTANT_String_info(i : Int) : CONSTANT_String_info = new CONSTANT_String_info(i)


	case class CONSTANT_Fieldref_info(
	  val	class_index : Int,
	  val name_and_type_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_Fieldref_info 

	def CONSTANT_Fieldref_info(
			class_index : Int, name_and_type_index : Int
	) : CONSTANT_Fieldref_info = new CONSTANT_Fieldref_info(class_index, name_and_type_index) 


	case class CONSTANT_Methodref_info(
	  val	class_index : Int,
	  val name_and_type_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_Methodref_info

	def CONSTANT_Methodref_info(
		class_index : Int, name_and_type_index : Int
	) : CONSTANT_Methodref_info = new CONSTANT_Methodref_info(class_index, name_and_type_index) 


	case class CONSTANT_InterfaceMethodref_info(
	  val	class_index : Int,
	  val name_and_type_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_InterfaceMethodref_info		

	def CONSTANT_InterfaceMethodref_info(
		class_index : Int, name_and_type_index : Int
	) : CONSTANT_InterfaceMethodref_info = 
		new CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index) 


	case class CONSTANT_NameAndType_info(
	  val	name_index : Int,
	  val descriptor_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_NameAndType_info	

	def CONSTANT_NameAndType_info(
		name_index : Int, descriptor_index : Int
	) : CONSTANT_NameAndType_info = new CONSTANT_NameAndType_info(name_index, descriptor_index) 
	
	
	case class CONSTANT_MethodHandle_info (
	 	val reference_kind : ReferenceKind.Value,
	   val reference_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_MethodHandle_info
	
	def CONSTANT_MethodHandle_info(
		reference_kind : Int, reference_index : Int
	) : CONSTANT_MethodHandle_info = new CONSTANT_MethodHandle_info(ReferenceKind(reference_kind), reference_index)
	
	
	case class CONSTANT_MethodType_info (
	 	val descriptor_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_MethodType_info
	
	def CONSTANT_MethodType_info(
		descriptor_index : Int
	) : CONSTANT_MethodType_info = new CONSTANT_MethodType_info(descriptor_index)
	
	
	case class CONSTANT_InvokeDynamic_info (
	 	val bootstrap_method_attr_index : Int,
	   val name_and_type_index : Int
	) extends de.tud.cs.st.bat.native.CONSTANT_InvokeDynamic_info
	
	def CONSTANT_InvokeDynamic_info(
		bootstrap_method_attr_index : Int, name_and_type_index : Int
	) : CONSTANT_InvokeDynamic_info = new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)
}


