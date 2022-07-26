/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
// TODO Create specialized IntResult and RefResult classes
sealed trait Result[@specialized(Int) +T] extends Serializable {
    final def isEmpty: Boolean = !hasValue
    def hasValue: Boolean
    def value: T
    def map[B](f: T => B): Result[B]
    def flatMap[B](f: T => Result[B]): Result[B]
    def foreach[U](f: (T) => U): Unit
    def withFilter(q: (T) => Boolean): Result[T]
    def toSet[X >: T]: Set[X]
    def toOption: Option[T]
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
            case Some(value) => Success(value)
            case _ /*None*/  => Empty
        }
    }

    def successOrFailure[T](result: Option[T]): Result[T] = {
        result match {
            case Some(value) => Success(value)
            case _ /*None*/  => Failure
        }
    }
}

/**
 * The computation '''succeeded''' and produced a result. In general
 */
case class Success[@specialized(Int) +T](value: T) extends Result[T] {

    class FilteredSuccess(p: (T) => Boolean) extends Result[T] {
        def hasValue: Boolean = p(value)
        def value: T = {
            if (hasValue) {
                value
            } else {
                throw new UnsupportedOperationException("the result is filtered")
            }
        }
        def foreach[U](f: (T) => U): Unit = if (p(value)) f(value)
        def map[B](f: (T) => B): Result[B] = if (p(value)) Success(f(value)) else Empty
        def flatMap[B](f: (T) => Result[B]): Result[B] = if (p(value)) f(value) else Empty
        def withFilter(p: (T) => Boolean): Result[T] = new FilteredSuccess((t: T) => p(t) && this.p(t))
        def toSet[X >: T]: Set[X] = if (p(value)) Set(value) else Set.empty
        def toOption: Option[T] = if (p(value)) Some(value) else None
    }

    def hasValue: Boolean = true
    def foreach[U](f: (T) => U): Unit = f(value)
    def map[B](f: (T) => B): Success[B] = Success(f(value))
    def flatMap[B](f: (T) => Result[B]): Result[B] = f(value)
    def withFilter(p: (T) => Boolean): Result[T] = new FilteredSuccess(p)
    def toSet[X >: T]: Set[X] = Set(value)
    def toOption: Option[T] = Some(value)
}

sealed trait NoResult extends Result[Nothing] {
    final def hasValue: Boolean = false
    final def value: Nothing = throw new UnsupportedOperationException("this result has no value")
    final def foreach[U](f: (Nothing) => U): Unit = {}
    final def map[B](f: (Nothing) => B): this.type = this
    final def flatMap[B](f: (Nothing) => Result[B]): this.type = this
    final def withFilter(q: (Nothing) => Boolean): this.type = this
    final def toSet[X >: Nothing]: Set[X] = Set.empty
    final def toOption: Option[Nothing] = None
}

object NoResult {
    def unapply(result: Result[_]): Boolean = !result.hasValue
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
