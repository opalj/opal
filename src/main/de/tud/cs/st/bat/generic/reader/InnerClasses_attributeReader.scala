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

import de.tud.cs.st.util.ControlAbstractions.repeat



/**

 * @author Michael Eichberg
 */
trait InnerClasses_attributeReader {
 

	type Constant_Pool
	type InnerClassesEntry
	implicit val InnerClassesEntryManifest : ClassManifest[InnerClassesEntry]
	type Attribute >: Null
	type InnerClasses_attribute <: Attribute
		
	type InnerClassesEntries = IndexedSeq[InnerClassesEntry]
	
	
	// a flavor of structural typing... when we mixin this trait this method needs to be available.
	def register(r : (String,(DataInputStream, Constant_Pool, Int) => Attribute)) : Unit
		
		
	def InnerClasses_attribute (
 		attribute_name_index : Int,classes : InnerClassesEntries
	)( implicit constant_pool : Constant_Pool) : InnerClasses_attribute
	
	
	def InnerClassesEntry (
		inner_class_info_index : Int,
		outer_class_info_index : Int,
		inner_name_index : Int,
		inner_class_access_flags : Int	
	)( implicit constant_pool : Constant_Pool) : InnerClassesEntry
	

	private lazy val reader = ( 
			de.tud.cs.st.bat.native.InnerClasses_attribute.name -> 
			((in : DataInputStream, cp : Constant_Pool, attribute_name_index : Int) => {
				val attribute_length = in.readInt()
				InnerClasses_attribute(
					attribute_name_index, 
					repeat(in.readUnsignedShort){
						InnerClassesEntry(
							in.readUnsignedShort, in.readUnsignedShort, 
							in.readUnsignedShort, in.readUnsignedShort
						)( cp )
					}
				)( cp )
			})
	);
	
	register(reader)
}
