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
/*
package org.opalj
package collection
package immutable

/**
 * Implementation of a immutable binary digital search tree (a Trie with M=2) that
 * is optimized for memory efficiency.
 *
 */
sealed trait TrieNode[+T] {

    def add[X >: T](value: X)(implicit key: Int): TrieNode[X]

    def foreach[U](f: (T) ⇒ U): Unit

    // Trie Management
    protected def left: TrieNode[T]
    protected def right: TrieNode[T]

}

case object EmptyTrie extends TrieNode[Nothing] {
    def foreach[U](f: (Nothing) ⇒ U): Unit = {}
    def add[X >: Nothing](value: X)(implicit key: Int): TrieNode[X] = new LTrieNode(value)
    protected def left: TrieNode[Nothing] = null
    protected def right: TrieNode[Nothing] = null
}

class LTrieNode[+T](val value: T) extends TrieNode[T] {
    def foreach[U](f: (T) ⇒ U): Unit = { f(value) }

    def add[X >: T](value: X)(implicit key: Int): TrieNode[X]

    protected def left = null
    protected def right = null
}

abstract class LOTrieNode[+T](
        val v:              T,
        protected var left: TrieNode[T]
) extends TrieNode[T] {
    protected def right = null
}

object TrieDemo extends App {

    class Store extends TrieNode[Int] {

    }

}
*/
