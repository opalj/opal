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

import TypeAliases._


/**
 * A class' inner classes.
 *
 * @author Michael Eichberg
 */
case class InnerClasses_attribute(
	val classes : InnerClassesEntries
) extends Attribute {

	def toXML =
		<inner_classes>
			{ for (clazz <- classes) yield clazz.toXML}
		</inner_classes>


	def toProlog[F,T,A <: T](
		factory : PrologTermFactory[F,T,A],
		declaringEntityKey : A
	) : List[F] =
		Nil
	/*	{
		import factory._

		Fact(
			"inner_classes",
			declaringEntityKey,
			Terms(classes,(_:InnerClassesEntry).toProlog(factory))
		) :: Nil
	}
	*/

}


case class InnerClassesEntry(
	val innerClassType : ObjectType,
	val outerClassType : ObjectType,
	val innerName : String,
	val innerClassAccessFlags : Int
) {

	import de.tud.cs.st.bat.canonical.AccessFlagsContext.INNER_CLASS
	import de.tud.cs.st.bat.canonical.AccessFlags

	def toXML =
		<class innerName={ innerName }>
			{	var nodes : List[scala.xml.Node] = Nil
				nodes = AccessFlags.toXML(innerClassAccessFlags, INNER_CLASS) :: nodes
				if (outerClassType != null) nodes = <outer_class type={outerClassType.className}/> :: nodes
				nodes = <inner_class type={innerClassType.className}/> :: nodes
				nodes
			}
		</class>

	/* 			// TODO implement toProlog
	def toProlog(
		factory : PrologTermFactory,
	) : GroundTerm = {
		import factory._
		Term(
			"inner_class",
		)
	}
	*/

}