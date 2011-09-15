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
 * A fact is a top level compound term. I.e., A fact is never a part of another compound term. Conceptually a 
 * fact is a clause where the right part (the part right to ":-") is always true. Modelling facts as ground terms 
 * facilitates unification.
 *
 * </p>
 * <p>
 * What needs to be consider when (dependencies between) facts are implemented?
 * <p>
 * Functionality that needs to be considerd:
 * <ol>
 * <li>
 * Efficient updating of the fact base has to be possible. That is, only those facts are touched (need to be) (removed / added) 
 * that do have changed. In general, the larger the set of facts that is touched the bigger the impact on the
 * whole program analyses.
 * </li>
 * <li>
 * Writing queries that can easily be written, comprehended, debugged and maintained.
 * </li>
 * <li>
 * Writing out the representation of the facts as true ISO Prolog facts to run analyses outside the static
 * analysis environment has to possible, but performance of the generation process is a non-issue and support
 * for this feature should not require any resources during normal operation!<br/>
 * <i>
 * This issues is of particular importance during the initial development (of the static analysis engine and ...) of the
 * static analyses to make sure that we can run the analysis in a trusted environment.
 * </i>
 * </li>	
 * </ol>
 * </p>
 * <p>	 
 * Dependencies between the facts are implemented using artificial key atoms which basically wrap the object 
 * that represents the referenced fact. These key atoms are created and maintaind by the fact that is uniquely 
 * identified using the key atom.
 * </p>
 *
 * @author Michael Eichberg
 */
trait Fact extends GroundTerm with Statement {

	override def toISOProlog : String = super.toISOProlog+"."
				
}

object Fact {
	
	def apply(func : String,terms : Array[GroundTerm]) : Fact = {
		new Fact() {
			
			type Functor = String
			
			def arity = terms.size
			
			def apply(i : Int) = terms(i)
						
			def functor = func
			
		}
	}
	
}

