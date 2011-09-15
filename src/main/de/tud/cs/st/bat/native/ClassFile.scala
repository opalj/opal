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
 * For details see the JVM Specification: The ClassFile Structure. 
 * <pre>
 * ClassFile { 
 * 	u4 magic; 
 * 	u2 minor_version; 
 * 	u2 major_version; 
 * 	u2 constant_pool_count; 
 * 	cp_info constant_pool[constant_pool_count-1]; 
 * 	u2 access_flags; 
 * 	u2 this_class; 
 * 	u2 super_class; 
 * 	u2 interfaces_count; 
 * 	u2 interfaces[interfaces_count]; 
 * 	u2 fields_count; 
 * 	field_info fields[fields_count]; 
 * 	u2 methods_count; 
 * 	method_info methods[methods_count]; 
 * 	u2 attributes_count; 
 * 	attribute_info attributes[attributes_count]; 
 * }
 * </pre>
 * This library supports class files from version 45 (Java 1.1) up to 
 * (including) version 60 (Java 6).
 *
 * @author Michael Eichberg
 */
trait ClassFile {

	//
	// ABSTRACT DEFINITIONS
	//

	type Constant_Pool_Entry <: de.tud.cs.st.bat.native.Constant_Pool_Entry
	type Fields // = Seq[Field_Info]
	type Methods // = Seq[Method_Info]
	type Attributes // = Seq[Attribute]
	type Interfaces 


	type Constant_Pool_Index = Int
	type Constant_Pool = IndexedSeq[Constant_Pool_Entry]

	
  	val minor_version : Int
  	val major_version : Int
  	val constant_pool : Constant_Pool
  	val access_flags : Int
  	val this_class : Int
  	val super_class : Int
  	val interfaces: Interfaces // Seq[Constant_Pool_Index] // references (int values) to constant pool entries
  	val fields : Fields
  	val methods : Methods
  	val attributes : Attributes
}


object ClassFile {
	
	val magic = 0xCAFEBABE
	
}