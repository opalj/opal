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
package de.tud.cs.st.bat.generic.reader

import java.io.DataInputStream


/**

 * @author Michael Eichberg
 */
trait Constant_PoolReader{

		
	//
	// ABSTRACT DEFINITIONS
	//
		
		
	type Constant_Pool_Entry 
	implicit val Constant_Pool_EntryManifest : ClassManifest[Constant_Pool_Entry]
	type CONSTANT_Class_info <: Constant_Pool_Entry
	type CONSTANT_Fieldref_info <: Constant_Pool_Entry
   type CONSTANT_Methodref_info <: Constant_Pool_Entry
  	type CONSTANT_InterfaceMethodref_info <: Constant_Pool_Entry
 	type CONSTANT_String_info <: Constant_Pool_Entry
 	type CONSTANT_Integer_info <: Constant_Pool_Entry
 	type CONSTANT_Float_info <: Constant_Pool_Entry
	type CONSTANT_Long_info <: Constant_Pool_Entry
 	type CONSTANT_Double_info <: Constant_Pool_Entry
 	type CONSTANT_NameAndType_info <: Constant_Pool_Entry
	private type T  = { def value : String } // a structural type
	type CONSTANT_Utf8_info <: Constant_Pool_Entry with T
	
	
	// FACTORY METHODS
	//
	def CONSTANT_Class_info(i : Int) : CONSTANT_Class_info
	def CONSTANT_Fieldref_info(class_index : Int, name_and_type_index : Int) : CONSTANT_Fieldref_info
	def CONSTANT_Methodref_info(class_index : Int, name_and_type_index : Int) : CONSTANT_Methodref_info
	def CONSTANT_InterfaceMethodref_info(class_index : Int, name_and_type_index : Int) : CONSTANT_InterfaceMethodref_info
	def CONSTANT_String_info(i : Int) : CONSTANT_String_info
	def CONSTANT_Integer_info(i : Int) : CONSTANT_Integer_info
	def CONSTANT_Float_info(f : Float) : CONSTANT_Float_info
	def CONSTANT_Long_info(l : Long) : CONSTANT_Long_info
	def CONSTANT_Double_info(d : Double) : CONSTANT_Double_info
	def CONSTANT_NameAndType_info(name_index : Int, descriptor_index : Int) : CONSTANT_NameAndType_info
	def CONSTANT_Utf8_info(s : String) : CONSTANT_Utf8_info
	

	//
	// IMPLEMENTATION
	//	
	
	import de.tud.cs.st.bat.native.Constant_Pool_Entry._ // CONSTANT_Class ... CONSTANT_Utf8
	
	type Constant_Pool = IndexedSeq[Constant_Pool_Entry]

	private val reader : Array[(DataInputStream)=>Constant_Pool_Entry] = new Array(13)
	
	reader(CONSTANT_Class) = (in : DataInputStream) 
			=> CONSTANT_Class_info(in.readUnsignedShort)
	
	reader(CONSTANT_Fieldref) = (in : DataInputStream) 
			=> CONSTANT_Fieldref_info(in.readUnsignedShort, in.readUnsignedShort)
  
	reader(CONSTANT_Methodref) = (in : DataInputStream) 
			=> CONSTANT_Methodref_info(in.readUnsignedShort, in.readUnsignedShort)

	reader(CONSTANT_InterfaceMethodref) = (in : DataInputStream) 
			=> CONSTANT_InterfaceMethodref_info(in.readUnsignedShort, in.readUnsignedShort)

	reader(CONSTANT_String) = (in : DataInputStream) 
			=> CONSTANT_String_info(in.readUnsignedShort)

	reader(CONSTANT_Integer) = (in : DataInputStream) 
			=> CONSTANT_Integer_info(in.readInt)

	reader(CONSTANT_Float) = (in : DataInputStream) 
			=> CONSTANT_Float_info(in.readFloat)

	reader(CONSTANT_Long) = (in : DataInputStream)
	 		=> CONSTANT_Long_info(in.readLong)

	reader(CONSTANT_Double) = (in : DataInputStream) 
			=> CONSTANT_Double_info(in.readDouble)

	reader(CONSTANT_NameAndType) = (in : DataInputStream) 
			=> CONSTANT_NameAndType_info(in.readUnsignedShort, in.readUnsignedShort)

	reader(CONSTANT_Utf8) = (in : DataInputStream) 
			=> CONSTANT_Utf8_info(in.readUTF)


	def Constant_Pool (in : DataInputStream) : Constant_Pool = {
		/*
		 * The value of the constant_pool_count item is equal to the 
		 * number of entries in the constant_pool table plus one. A 
		 * constant_pool index is considered valid if it is greater than zero 
		 * and less than constant_pool_count     
		 */
		val constant_pool_count = in.readUnsignedShort
		/*
		 * The format of each constant_pool 
		 * table entry is indicated by its ﬁrst “tag” byte. 
		 * The constant_pool table is indexed from 1 to constant_pool_count−1.
		 */
		val constant_pool_entries  = new Array[Constant_Pool_Entry](constant_pool_count)
		var i = 1
		while (i < constant_pool_count) {
			val tag = in.readUnsignedByte 
			val constantReader = reader(tag)
			val constantPoolEntry = reader(tag)(in)
			constant_pool_entries(i) = constantPoolEntry
			tag match { 
				case CONSTANT_Long => i += 2
				case CONSTANT_Double => i += 2
				case _ => i += 1
			}
    	}
		constant_pool_entries
	}
}
