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
 * Implementation of some convenience methods over Arrays.
 *
 * @author Michael Eichberg
 */
object Arrays {
	
	/**
	 * Given a sorted array in ascending order and a value that should be 
	 * inserted into the array, this method returns the index where to store 
	 * the new value.<br />
	 * If the given value is already stored in this sorted array, the index -1 is
	 * returned. 
	 * 
	 * @param 	a a sorted array in ascending order in which to search for the given value.
	 * @param 	upperBound if only the beginning of the array should be used it is possible
	 * 			to specify an upper bound to limit the search to the range [0..upperBound).
	 * 			Hence, if you want to search for a value in an entire Array upperBound
	 *				has to be equal to <code>a.size</code>.
	 */
	def getInsertionIndex[T <% Int](a : Array[T],upperBound : Int,value : T) : Int = {
		
		// binary search method
		
		var max_index 	= upperBound
		var min_index  = 0
		while (min_index < max_index) { 
			val index 		= (min_index + max_index) / 2
			val v 			= a(index)
			if (v == value) return -1
			else if (value > v) min_index = index+1
			else max_index = index
		}
		return min_index
	}

}