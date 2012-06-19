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
package de.tud.cs.st.prolog

/**
 * An atom is an immutable term with no argument.
 * <p>
 * The static analysis engine distinguishes / supports four types of atoms:
 * <ul>
 * <li><b>TextAtoms</b> - where the type of the functor is a String object</li>
 * <li><b>IntegerAtoms</b> - where the type of the functor is Long</li>
 * <li><b>FloatAtoms</b> - where the type of the functor is Double</li>
 * <li><b>KeyAtoms</b> - where the type of the functor is an arbitrary object. The only operation that is supported on
 * a key atom is the (reference) equality check (on the underlying object).</li>
 * </ul> 
 * </p>
 *
 * @author Michael Eichberg
 */
trait Atom extends GroundTerm {
	
	import Atom._

	
	def category : ATOM_TYPE
	
	
	/**
	 * The type of the functor. By making the type abstract a functor can be, e.g., an object that references another
	 * fact. For details see {@link KeyAtom}.
	 */
	type Functor 


	/**
	 * The functor of an atom represents the value of the atom. 
	 */
	def functor : Functor


	//
	// CONCRETE IMPLEMENTATION
	//
	
		
	/** 
	 * Returns "0"; an atom's arity is by definition zero. 
	 */
	final def arity : Int = 0

				
	/**
	 * Always throws an <code>ArrayIndexOutOfBoundException</code> since atoms do not have a body. 
	 */
	final def apply(i : Int) = throw new ArrayIndexOutOfBoundsException("An atom does not have a body.")

	
	override def toISOProlog : String = "\'"+ISOProlog.escapeString(functor.toString)+"\'"
	
}
object Atom {
	
	type ATOM_TYPE = Int
	
	val TEXT_ATOM : ATOM_TYPE = 0
	val INTEGER_ATOM : ATOM_TYPE = 1
	val FLOAT_ATOM : ATOM_TYPE = 2
	val KEY_ATOM : ATOM_TYPE = 3
	
}

