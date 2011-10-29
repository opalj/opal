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
package de.tud.cs.st.util.perf


import scala.collection.mutable.Map


/**
 * Trait that defines methods for measuring the execution time of some code.
 *
 * @author Michael Eichberg
 */
trait PerformanceEvaluation extends BasicPerformanceEvaluation {


	/**
	 * Times the execution of a given method (function literal) / code block.
	 * @param s a string that identifies the code/the method that is measured. <br/>
	 * E.g. <i>"calculation of the fibonacci number".</i>
	 */
	def time[T](s : String)(f : => T ) : T




	/**
	 * Times the execution of the given method / function literal / code block and
	 * adds it to the execution time of previous methods / function literals/ code blocks
	 * that were measured and for which the same symbole was used. <br/>
	 * E.g. <code>aggregateTimes('base_analysis){ ... do something ... }</code>
	 *
	 * @param s symbol used to put multiple measurements into relation. 
	 */
	def aggregateTimes[T](s : Symbol)(f : => T) : T 
	
	def printAggregatedTimes(s:Symbol, description:String)
	
	def resetAggregatedTimes(s:Symbol) 
}