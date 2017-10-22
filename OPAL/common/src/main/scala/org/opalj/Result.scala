/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj

/**
 * Represents the result of some expression that either (a) succeeded and encapsulates some value,
 * or (b) finished, but has no value - because it was not possible to compute a value using the
 * given/available information - or (c) that failed.
 *
 * @note    Depending on the context, it may be useful to distinguish between a success that returns
 *          an empty collection and a success that has no further information.
 *
 * @author  Michael Eichberg
 */
sealed trait Result[@specialized(Int) +T] extends Serializable {
    final def isEmpty: Boolean = !hasValue
    def hasValue: Boolean
    def value: T
    def map[B](f: T ⇒ B): Result[B]
    def flatMap[B](f: T ⇒ Result[B]): Result[B]
    def foreach[U](f: (T) ⇒ U): Unit
    def withFilter(q: (T) ⇒ Boolean): Result[T]
    def toSet[X >: T]: Set[X]
}

/**
 * Defines factory methods for [[Result]] objects.
 *
 * @author Michael Eichberg
 */
object Result {

    /**
     * Maps a `Some` to [[Success]] and `None` to [[Empty$]].
     */
    def apply[T](result: Option[T]): Result[T] = {
        result match {
            case Some(value) ⇒ Success(value)
            case _ /*None*/  ⇒ Empty
        }
    }

    def successOrFailure[T](result: Option[T]): Result[T] = {
        result match {
            case Some(value) ⇒ Success(value)
            case _ /*None*/  ⇒ Failure
        }
    }
}

/**
 * The computation '''succeeded''' and produced a result. In general
 */
case class Success[@specialized(Int) +T](value: T) extends Result[T] {

    class FilteredSuccess(p: (T) ⇒ Boolean) extends Result[T] {
        def hasValue: Boolean = p(value)
        def value: T = {
            if (hasValue) {
                value
            } else {
                throw new UnsupportedOperationException("the result is filtered")
            }
        }
        def foreach[U](f: (T) ⇒ U): Unit = if (p(value)) f(value)
        def map[B](f: (T) ⇒ B): Result[B] = if (p(value)) Success(f(value)) else Empty
        def flatMap[B](f: (T) ⇒ Result[B]): Result[B] = if (p(value)) f(value) else Empty
        def withFilter(p: (T) ⇒ Boolean): Result[T] = new FilteredSuccess((t: T) ⇒ p(t) && this.p(t))
        def toSet[X >: T]: Set[X] = if (p(value)) Set(value) else Set.empty
    }

    def hasValue: Boolean = true
    def foreach[U](f: (T) ⇒ U): Unit = f(value)
    def map[B](f: (T) ⇒ B): Success[B] = Success(f(value))
    def flatMap[B](f: (T) ⇒ Result[B]): Result[B] = f(value)
    def withFilter(p: (T) ⇒ Boolean): Result[T] = new FilteredSuccess(p)
    def toSet[X >: T]: Set[X] = Set(value)
}

sealed trait NoResult extends Result[Nothing] {
    final def hasValue: Boolean = false
    final def value: Nothing = throw new UnsupportedOperationException("this result has no value")
    final def foreach[U](f: (Nothing) ⇒ U): Unit = {}
    final def map[B](f: (Nothing) ⇒ B): this.type = this
    final def flatMap[B](f: (Nothing) ⇒ Result[B]): this.type = this
    final def withFilter(q: (Nothing) ⇒ Boolean): this.type = this
    final def toSet[X >: Nothing]: Set[X] = Set.empty
}

object NoResult {
    def unapply(noResult: NoResult): Boolean = true
}

/**
 * The computation '''finished''', but did no produce any results or the result was filtered.
 *
 * @note    The precise semantics of ''succeeded without results'' is dependent on the semantics
 *          of the concrete computation and needs to be defined per use case.
 */
case object Empty extends NoResult

/**
 * The computation '''failed''' because of missing/incomplete information.
 *
 * @note    The precise semantics of the ''computation failed'' is dependent on the semantics
 *          of the concrete computation and needs to be defined per use case.
 */
case object Failure extends NoResult
