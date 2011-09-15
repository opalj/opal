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

// TODO extract the definition for arbitrary (non-ground) terms.

/**
 * A list of terms. 
 * <p>
 * Conceptually, a list of three terms [a,b,c] is considered to be 
 * the following ground term: cons(a,cons(b,cons(c,nil))). However,
 * special methods to directly access the members of the list are 
 * provided.<br />
 * Prolog's native representation using cons and nil for list of terms
 * is returned / created when the standard apply and arity functions
 * are used.
 * </p>
 *
 * @author Michael Eichberg
 */
abstract class GroundTerms[+T <: GroundTerm] protected[prolog]() extends GroundTerm {


	//
	// The following methods support direct access to the elements of this list 
	// of terms. These method are to be used by the SAE to access a GroundTerms'  
	// arguments.
	// 
		
	/**
	 * Number of elements of the list.
	 */
	def size : Int
	
	/**
	 * Returns the ith member of this list of terms.
	 */
	def terms (i : Int) : T	
				
	
	override def equals (a : Any) : Boolean = a match {
		case other : GroundTerms[_] =>
			if (other.size == this.size) {
				val max = size
				var i = 0
				while (i < max) {
					if (other.terms(i) != this.terms(i)) return false
					i += 1
				}
				true
			}
			else {
				false
			}
		case _ => 
			false
	}
	
	
	override def hashCode : Int = {
		val max = size
		var hc = 17 * (functor.hashCode + max + 17)
		var i = 0
		while (i < max) {
			hc = 17 * (hc + terms(i).hashCode)
			i += 1
		}
		hc
	}
	
	
	override def toISOProlog() : String = {	
		var i = 0
		var s = "["
		while(i < size){
			s += terms(i).toISOProlog
			i += 1
			if (i < size) s += ","
		}
		s += "]"
		s
	}
	
	
	//
	// Debugging functionality.
	//

	override def toString = "GroundTerms(arguments="+toISOProlog+")"

	final type Functor = String

	def functor : Functor = "cons" // cons(<HEAD>,<TAIL>)
	
	def arity = 2
	
	def apply (i : Int) : GroundTerm = { 
		System.err.println("[Warning] Debugging method used: \"(Ground)Terms.apply\"")
		i match {
			case 0 => terms(index)
			case 1 => if (index + 1 < size) new cons(index + 1,this) else nil
		}
	}
	
	protected def index = 0
	
	def toCompoundTermForm : String = {
		functor +"("+apply(0).toISOProlog +","+apply(1).toISOProlog+")"
	}

}
/**
 * Implements several helper methods to create lists of ground terms.
 *
 * @author Michael Eichberg
 */
object GroundTerms {
	
	def apply[T <: GroundTerm : ClassManifest](termsArray : Array[T]) = 
		if (termsArray.size == 0)
			nil
		else
			new GroundTerms[T] {  
				def size : Int = termsArray.size
				def terms (i : Int) : T = termsArray(i)
			}


	/**
	 * @param s a sequence of arbitrary values of type t.
	 * @param f a function that given a value of type t returns a term with type <code>GroundTerm</code>.
	 */
	def randomAccessSeqToTerms[T, G <: GroundTerm : ClassManifest](s : IndexedSeq[T], f : T => G) : GroundTerms[G] = {
		if (s.isEmpty)
			/*prolog.terms*/nil
		else {
			val terms = new Array[G](s.size)
			var i = s.size-1
			while (i >= 0) {
				terms(i) = f(s(i))
				i -= 1
			}
			GroundTerms(terms)
		}
	}	
	
	
	def seqToTerms[T, G <: GroundTerm : ClassManifest](s : Seq[T], f : T => G) : GroundTerms[G] = {
		if (s.isEmpty)
			/*prolog.terms*/nil
		else {
			val terms = new Array[G](s.size)
			var i = 0
			for (e <- s) {
				terms(i) = f(e)
				i += 1
			}
			GroundTerms(terms)
		}
	}
}


/**
 * A term where the first parameter is an element of a list and the second parameter is the rest of the list.
 *
 * @author Michael Eichberg
 */
final class cons[+T <: GroundTerm] private[prolog](
	override val index : Int,
	val ts : GroundTerms[T]
) extends GroundTerms[T] {
	
	def size = ts.size - index
	
	def terms(i : Int) = ts.terms(i+index)
	
	override def equals (a : Any) : Boolean = sys.error("not implemented")
	
	override def hashCode : Int = sys.error("not implemented")
}


/**
 * The empty list of terms.
 *
 * <p><font color="red">
 * Implementation restriction: <br/>
 * The empty list of terms must be represented using the object <code>nil</code>!
 * </font></p>
 *
 * @author Michael Eichberg
 */
object nil extends GroundTerms[Nothing] {

	/** The functor of this compound term. */
	override def functor : Functor = "nil"
	
	def size = 0
	
	def terms(i : Int) : Nothing = throw new ArrayIndexOutOfBoundsException()
	
	override def equals (a : Any) : Boolean = a.asInstanceOf[Object] eq this
	/* a match {
		case other : nil => other eq this
		case _ => false
	}*/
	
	override def hashCode : Int = 0
		
	override def toISOProlog : String = "[]"
}





