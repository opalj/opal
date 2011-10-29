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
package de.tud.cs.st.util.collection


/**
 * A stripped down variant of scala's default "IndexedSeq" trait that
 * does not specify / implement operations that are (potentially) expensive.
 * 
 * @author Michael Eichberg
 */
trait Indexed[A] {
	
		
	// ABSTRACT DEFINITIONS
	
	def size : Int
	
	def apply(i : Int) : A
		
	
	// CONCRETE IMPLEMENTATIONS
	
	def mkString(f : A => String, separator : String) : String = {
		if (size == 0) {
			""
		} else {
			val s = new StringBuilder(f(this(0)))
			var i = 1
			while (i < size) {
				s.append(separator)
				s.append(f(this(i)))
				i += 1
			}
			s.toString			
		}		
	}	
}

object Indexed {
	
	
	/**
	 * Combines the elements of two equally sized indexed data structures. Returns false if the combination of two 
	 * entries fails or if the size of the structures is different.
	 */
	def combine[X,Y](i1 : Indexed[X], i2 : Indexed[Y], f : (X,Y) => Boolean) : Boolean = {
		i1.size == i2.size && {
			val max = i1.size
			var i = 0
			while (i < max && f(i1(i),i2(i))) { i+= 1 }
			i == max
		}
	}
	
}