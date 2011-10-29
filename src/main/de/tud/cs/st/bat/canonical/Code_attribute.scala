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
 * <pre>
 * Code_attribute { 
 * 	u2 attribute_name_index; 
 * 	u4 attribute_length; 
 * 	u2 max_stack; 
 * 	u2 max_locals; 
 * 	u4 code_length; 
 * 	u1 code[code_length]; 
 * 	u2 exception_table_length; 
 * 	{	u2 start_pc; 
 * 		u2 end_pc; 
 * 		u2 handler_pc; 
 * 		u2 catch_type; 
 * 	} exception_table[exception_table_length]; 
 * 	u2 attributes_count; 
 * 	attribute_info attributes[attributes_count]; 
 * } 
 * </pre>
 *
 * @author Michael Eichberg
 */
trait Code_attribute extends Attribute{

	//
	// ABSTRACT DEFINITIONS
	// 
	
	type Code
	type ExceptionTableEntry
	type Attributes
	

	val attribute_name_index : Int
	val attribute_length : Int
	val max_stack : Int
	val max_locals : Int
	val code :Code
	val exception_table : ExceptionTable
	val attributes : Attributes

	//
	// CONCRETE (FINAL) DEFINITIONS
	// 

	type ExceptionTable = Seq[ExceptionTableEntry]

	def attribute_name = Code_attribute.name
}
object Code_attribute{
	
	val name = "Code"
	
}


trait ExceptionTableEntry {

	val start_pc: Int

	val end_pc: Int

	val handler_pc : Int

	val catch_type : Int
}
