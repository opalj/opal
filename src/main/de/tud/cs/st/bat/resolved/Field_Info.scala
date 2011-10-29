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


import scala.xml.Elem
import scala.xml.Null
import scala.xml.Text
import scala.xml.TopScope

import TypeAliases._


/**
 * Represents a single field declaration.
 *
 * @author Michael Eichberg
 */
case class Field_Info (
	val accessFlags : Int,
  	val name : String,
  	val descriptor : FieldDescriptor,
  	val attributes : Attributes
) { 

	import de.tud.cs.st.bat.canonical.AccessFlagsContext.FIELD
	import de.tud.cs.st.bat.canonical.AccessFlagsIterator
		
	def toXML = 
		<field 		
			name={ name } 
			type={ descriptor.fieldType.toJava } >
			<flags>{ AccessFlagsIterator(accessFlags, FIELD) map ( (f) => Elem(null,f.toString,Null,TopScope) ) }</flags>
			<attributes>{ for (attribute <- attributes) yield attribute.toXML }</attributes>	
		</field>
		
		
	def toProlog[F,T,A <: T](
		factory : PrologTermFactory[F,T,A],
		classFileKeyAtom : A
	) : List[F] = {

		import factory._

		var facts : List[F] = Nil

		val key = KeyAtom("f_")
		
		for (attribute <- attributes) { 
			facts = (attribute match {
				case aa : Annotations_Attribute => aa.toProlog(factory,key)
				case cva : ConstantValue_attribute => cva.toProlog(factory,key)
				case _ => Nil
			}) ::: facts
		}

		Fact(
			"field",	// functor
			classFileKeyAtom,
			key,
			TextAtom(name),
			descriptor.toProlog(factory),
			VisibilityAtom(accessFlags,FIELD),
			FinalTerm(accessFlags),
			StaticTerm(accessFlags),
			TransientTerm(accessFlags),
			VolatileTerm(accessFlags),
			SyntheticTerm(accessFlags,attributes),
			DeprecatedTerm(attributes)
		) :: facts

	}		
}