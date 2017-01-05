/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
//package org.opalj
//package collection
//package mutable
//
//import scala.collection
//import scala.collection.immutable
//
///**
// * A set of int values which is optimized for storing a small number of small(er) int values.
// *
// * @author Michael Eichberg
// */
//abstract class PackedIntSet 
//extends immutable.Set[Int]
//        with collection.SetLike[Int, immutable.Set[Int]]
//        //with mutable.Cloneable[PackedIntSet]
//        with Serializable { 
//
//  def size: Int
//    def contains(i: Int): Boolean
//    def foreach[U](f: Int ⇒ U): Unit
//    def iterator: Iterator[Int]
//
//  def +(i: Int): PackedIntSet
//    def -(i: Int): PackedIntSet
//    
//    /**
//     * Adds the given value to `this` set if `this` set has some space left; if this set is  
//     * already full, a new set is created which can hold this additional value and that set 
//     * is returned.
//     */
//    def +≈(i : Int): PackedIntSet
//    
//    final override def empty : PackedIntSet = EmptyPackedIntSet
//}
//
//private object EmptyPackedIntSet extends PackedIntSet {
//    override def isEmpty = true
//    override def nonEmpty = false
//  override def size: Int = 0
//          override def contains(i: Int): Boolean = false
//          override def foreach[U](f: Int ⇒ U): Unit = {}
//    override def iterator: Iterator[Int] = Iterator.empty
//
//    override def +(i: Int): PackedIntSet = new PackedIntSet1(i)
//    override def -(i: Int): this.type = this
//}
//
//private abstract class NonEmptyPackedIntSet extends PackedIntSet {
//    final override def isEmpty = false
//    final override def nonEmpty = true
//}
//
//private final class PackedIntSet1 (val i1 : Int) extends NonEmptyPackedIntSet {
//  override def size: Int = 1
//          override def contains(i: Int): Boolean = i == i1
//          override def foreach[U](f: Int ⇒ U): Unit = f(i1)
//          override def iterator: Iterator[Int] = Iterator.single(i1) 
//    
//          override def -(i: Int): PackedIntSet = if(i == i1) EmptyPackedIntSet else this
//    override def +(i: Int): PackedIntSet = {
//        if(i == i1)
//            return;
//        
//    }
//}
//
///**
// * A non-empty set of byte values.
// */
//private final class BytePackedIntSet(val values : Int) extends PackedIntSet {
//    
//}
//
////private final class 
//
//object PackedIntSet{
//    
//    def empty : PackedIntSet = EmptyPackedIntSet 
//    
//    def apply(i : Int) : PackedIntSet = {
//        ???
//    }
//}