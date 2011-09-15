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
 * An immutable store.
 *
 * @author Michael Eichberg
 */
trait Store[+A] {
  
  def v : A // we have to use "def" in order to be able to override it in EmptyStore
  
  /** Creates a new store which contains all elements of this store and the additional element. */
  def +[B >: A] (b : B) : Store[B]
    
  /** Removes some element and returns the element as well as the newly created store without the element. */
  def ↓ : (A,Store[A])
  
  def contains(a : Any) : Boolean
  
  def foreach (f: (A) => Unit) : Unit 
  
  def isEmpty : Boolean
  
  def size : Int 
  
  override def equals(other : Any) : Boolean = sys.error("needs to be implemented")

  override def hashCode : Int = sys.error("needs to be implemented")
}


private[collection] class LinkedListStore[+A] (val v : A, val rest : Store[A]) extends Store[A] {
    
  def this(v : A) { this (v,EmptyStore) }
  
  def +[B >: A] (b : B) : LinkedListStore[B] = new LinkedListStore(b,this)
  
  def ↓ : (A,Store[A]) = (v,rest)
 
  def size : Int = 1 + rest.size 
  
  def isEmpty = false // don't use a "val" here... this would lead to a new field!
  
  def contains(a : Any) : Boolean = this.v == a || (rest contains a)
 
  def foreach (f : (A) => Unit) { f(v) ; (rest foreach f) }
  
  override val hashCode = v.hashCode + rest.hashCode
  
  override def equals (other : Any) : Boolean = other match {
    case EmptyStore => false
    case that : Store[_] => {
         foreach((a) => if (!(that contains a)) return false)
         true
      } 
    case _ => false
  }
  
  override def toString =  v + " => "+ rest.toString 
}


object EmptyStore extends Store[Nothing] {
  /*
  trait Store takes type parameters	ScalaDemo/src/collection/Store.scala
  */
  
  val size = 0
  
  override def v() : Nothing = sys.error("the store is empty")
  
  def +[B] (b : B) : Store[B] = new LinkedListStore(b)
    
  def ↓ = sys.error("the store is empty")
  
  def foreach(f : (Nothing) => Unit) : Unit = {}
  
  def contains(a : Any) = false
  
  val isEmpty = true
  
  override val hashCode : Int = -1; 
  
  override def equals (other : Any) : Boolean = other match {
    case that : Store[_] => that.isEmpty 
    case _ => false
  }
  
  override def toString() = "∅"
}


object Store {
  
  def apply[A] (a : A*) : Store [A] = (EmptyStore.asInstanceOf[Store[A]] /: a)(_ + _)
   
}