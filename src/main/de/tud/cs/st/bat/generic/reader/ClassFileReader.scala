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

import java.io.{InputStream,DataInputStream,BufferedInputStream}

import de.tud.cs.st.util.ControlAbstractions.repeat

import de.tud.cs.st.bat.canonical.ClassFile.magic



/**

 * @author Michael Eichberg
 */
trait ClassFileReader extends Constant_PoolAbstractions{ 	// TODO Split up the ClassFileReader in a ClassFileReader + FieldReader + MethodReader
	
	
	//
	// ABSTRACT DEFINITIONS
	//
	
	
	type ClassFile
	type Fields
	type Methods
	type Attributes
	

	type Interfaces = IndexedSeq[Constant_Pool_Index]


	// METHODS DELEGATING TO OTHER READERS
	//

	def Constant_Pool(in : DataInputStream) : Constant_Pool
	
	def Fields(in : DataInputStream, cp : Constant_Pool) : Fields
	
	def Methods(in : DataInputStream, cp : Constant_Pool) : Methods
	
	def Attributes(in : DataInputStream, cp : Constant_Pool)	: Attributes

	
	// FACTORY METHOD(S) TO CREATE (AN) OBJECT(S) THAT REPRESENT(S) A PART OF A CLASS FILE 
	//

	def ClassFile(
		//cp : Constant_Pool
		minor_version : Int, major_version : Int, 
		access_flags : Int, this_class : Constant_Pool_Index, super_class : Constant_Pool_Index,
		interfaces : Interfaces, fields : Fields, methods : Methods, 
		attributes : Attributes
	) (implicit	cp : Constant_Pool) : ClassFile


	
	//
	// IMPLEMENTATION
	//	
	

	final def ClassFile (create : () => InputStream) : ClassFile = {
		var in = create();
		if (!in.isInstanceOf[DataInputStream]) {
			if (!in.isInstanceOf[BufferedInputStream]) {
				in = new BufferedInputStream(in)
			}
			in = new DataInputStream(in)
		}
    	try {
			ClassFile(in.asInstanceOf[DataInputStream])
		} finally {
			in.close
		}
	}


	def ClassFile (in : DataInputStream) : ClassFile = {
		// magic
		require (magic == in.readInt, "No class file.")
  
		val minor_version = in.readUnsignedShort // minor_version
		val major_version = in.readUnsignedShort // major_version

		// let's make sure that we support this class file's version
		require (major_version >= 45 && // at least JDK 1.1.
					(major_version < 51 || 
					 (major_version == 51 && minor_version == 0))) // Java 6 = 50.0; Java 7 == 51.0		
		
		implicit val cp = Constant_Pool(in) 
		val access_flags = in.readUnsignedShort // access_flags
		val this_class = in.readUnsignedShort // this_class
		val super_class = in.readUnsignedShort // super_class
		val interfaces = { // interfaces
			val interfaces_count = in.readUnsignedShort;
			repeat(interfaces_count){in.readUnsignedShort}
		}
		val fields = Fields(in,cp) // fields
		val methods = Methods(in,cp) // methods
		val attributes = Attributes(in,cp) // attributes
   
		ClassFile(
			minor_version, major_version, 
			access_flags, this_class, super_class,
			interfaces, fields, methods, attributes 
		)
	}
}