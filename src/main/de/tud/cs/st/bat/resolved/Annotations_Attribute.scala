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
 * A class, method, or field annotation.
 *
 * @author Michael Eichberg
 */
trait Annotations_Attribute extends Attribute { // TODO inconsistent naming: choose either AnnotationsAttribute or Annotations_attribute...

	def annotations : Annotations
	
	def annotationsToXML = for (annotation <- annotations) yield annotation.toXML
	
	def isRuntimeVisible : Boolean

	// FIXME Is the following correct? The key of an annotation fact is composed out of the (reference)keyAtom and the annotationTypeTerm. Every Annoation only allowed to appear once (in Java source code this is the case, but what does the JVM Spec say?)	
	def toProlog[F,T,A <: T](
		factory : PrologTermFactory[F,T,A],
		declaringEntityKey : A
	) : List[F] = {
	
		import factory._
	
		var facts : List[F] = Nil
	
		for (annotation <- annotations) {
			facts = Fact(
				"annotation",
				declaringEntityKey,
				if (isRuntimeVisible) 
					StringAtom("runtime_visible")
				else
					StringAtom("runtime_invisible"),
				annotation.annotationType.toProlog(factory),
				factory.Terms(
					annotation.elementValuePairs,
					(_ : ElementValuePair).toProlog(factory)
				)	
			) :: facts
		}
		facts
		
		/*
		Fact(
			"annotations",
			declaringEntityKey,
			if (isRuntimeVisible) 
				StringAtom("runtime_visible")
			else
				StringAtom("runtime_invisible"),
			Terms(annotations,(_ : Annotation).toProlog(factory))
		) :: facts	
		*/
	}
	
}
