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
 * Each term has a functor and a body.
 *
 * @param <T> The type of this term's terms; i.e. the common
 *		supertype of the types of the body's terms.
 *
 * @author Michael Eichberg
 */
trait Term extends ISOProlog {

	
	type Functor


	/** The functor of this (compound) term. */
	def functor : Functor
		
		
	/** The arity of this term; i.e. the number of terms the body consists of / the number of parameters of this term. */
	def arity : Int 

				
	/** 
	 * Returns the ith term of the body of this compound term (zero based).
	 * <p> 
	 * <i>If this term does not have at least i+1
	 * parameters, the result is undetermined. A class implementing this method is free to return an arbitrary value
	 * or to throw an exception.</i>
	 * </p>
	 */
	def apply(i : Int) : Term


	/** 
	 * @return <code>true</code> if this term is ground, <code>false</code> otherwise.
	 */
	def isGround : Boolean
	

	//
	// IMPLEMENTATION 
	//
	
	/** 
	 * Converts this term to an ISO Prolog term. This method assumes that a functor never needs to be escaped.
	 */
	def toISOProlog : String = {
		var s = functor.toString+"("
		var i = 0
		while (i < arity) {
			s += apply(i).toISOProlog
			i += 1
			if (i < arity) s += ","
		}
		s += ")"
		s
	}
	
}

