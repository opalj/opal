/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.ClassTag

import scala.collection.immutable.ArraySeq
import scala.quoted.*

/**
 * Defines common control abstractions.
 *
 * @author Michael Eichberg
 */
package object control {

    /**
     * Iterates over a given array `a` and calls the given function `f` for
     * each non-null value in the array.
     *
     * @note '''This is a macro.'''
     */
    inline final def foreachNonNullValue[T <: AnyRef](
        a: Array[T]
    )(
        f: (Int, T) => Unit
    ): Unit = ${ ControlAbstractionsImplementation.foreachNonNullValue[T]('a)('f) }

    /**
     * Allocation free, local iteration over all elements of an array.
     *
     * @note '''This is a macro.'''
     */
    inline final def foreachWithIndex[T <: AnyRef](
        a: Array[T]
    )(
        f: (T, Int) => Unit
    ): Unit = ${ ControlAbstractionsImplementation.foreachWithIndex[T]('a)('f) }

    /**
     * Executes the given function `f` for the first `n` values of the given list.
     * The behavior is undefined if the given list does not have at least `n` elements.
     *
     * @note '''This is a macro.'''
     */
    inline final def forFirstN[T <: AnyRef](
        l: List[T],
        n: Int
    )(
        f: T => Unit
    ): Unit = ${ ControlAbstractionsImplementation.forFirstN[T]('l, 'n)('f) }

    /**
     * Evaluates the given expression `f` with type `T` the given number of
     * `times` and stores the result in a [[scala.collection.immutable.ArraySeq]].
     *
     * ==Example Usage==
     * {{{
     * val result = repeat(15) {
     *      System.in.read()
     * }
     * }}}
     *
     * @note '''This is a macro.'''
     *
     * @param times The number of times the expression `f` is evaluated. The `times`
     *      expression is evaluated exactly once.
     * @param f An expression that is evaluated the given number of times unless an
     *      exception is thrown. Hence, even though `f` is not a by-name parameter,
     *      it behaves in the same way.
     * @return The result of the evaluation of the expression `f` the given number of
     *      times stored in an `IndexedSeq`. If `times` is zero an empty sequence is
     *      returned.
     */
    def fillArraySeq[T <: AnyRef](
        times: Int
    )(
        f: => T
    )(implicit classTag: ClassTag[T]): ArraySeq[T] = ArraySeq.fill(times)(f)

    // macro ControlAbstractionsImplementation.fillRefArray[T]
    // OLD IMPLEMENTATION USING HIGHER-ORDER FUNCTIONS
    // (DO NOT DELETE - TO DOCUMENT THE DESIGN DECISION FOR MACROS)
    //        def repeat[T](times: Int)(f: => T): IndexedSeq[T] = {
    //            val array = new scala.collection.mutable.ArrayBuffer[T](times)
    //            var i = 0
    //            while (i < times) {
    //                array += f
    //                i += 1
    //            }
    //            array
    //        }
    // The macro-based implementation has proven to be approx. 1,3 to 1,4 times faster
    // when the number of times that we repeat an operation is small (e.g., 1 to 15 times)
    // (which is very often the case when we read in Java class files)

    /**
     * Evaluates the given expression `f` the given number of
     * `times` and stores the result in an [[scala.collection.immutable.ArraySeq[Int]].
     *
     * ==Example Usage==
     * {{{
     * val result = fillIntArray(15) { System.in.readByte() }
     * }}}
     *
     * @note '''This is a macro.'''
     *
     * @param times The number of times the expression `f` is evaluated. The `times`
     *      expression is evaluated exactly once.
     * @param f An expression that is evaluated the given number of times unless an
     *      exception is thrown. Hence, even though `f` is not a by-name parameter,
     *      it behaves in the same way.
     * @return The result of the evaluation of the expression `f` the given number of
     *      times stored in an `IndexedSeq`. If `times` is zero an empty sequence is
     *      returned.
     */
    inline def fillIntArray(
        times: Int
    )(
        f: => Int
    ): ArraySeq[Int] = ${ ControlAbstractionsImplementation.fillIntArray('times)('f) }

    inline def fillArrayOfInt(
        times: Int
    )(
        f: => Int
    ): Array[Int] = ${ ControlAbstractionsImplementation.fillArrayOfInt('times)('f) }

    /**
     * Iterates over the given range of integer values `[from,to]` and calls the given
     * function f for each value.
     *
     * If `from` is smaller or equal to `to`, `f` will not be called.
     *
     * @note '''This is a macro.'''
     */
    inline def iterateTo(
        from: Int,
        to:   Int
    )(
        f: Int => Unit
    ): Unit = ${ ControlAbstractionsImplementation.iterateTo('from, 'to)('f) }

    /**
     * Iterates over the given range of integer values `[from,until)` and calls the given
     * function f for each value.
     *
     * If `from` is smaller than `until`, `f` will not be called.
     */
    inline def iterateUntil(
        from:  Int,
        until: Int
    )(
        f: Int => Unit
    ): Unit = ${ ControlAbstractionsImplementation.iterateUntil('from, 'until)('f) }

    /**
     * Runs the given function f the given number of times.
     */
    inline def repeat(times: Int)(f: => Unit): Unit = ${ ControlAbstractionsImplementation.repeat('times)('f) }

    /**
     * Finds the value identified by the given comparator, if any.
     *
     * @note    The comparator has to be able to handle `null` values if the given array may
     *          contain null values.
     *
     * @note    The array must contain less than Int.MaxValue/2 values.
     *
     * @param   data An array sorted in ascending order according to the test done by the
     *          comparator.
     * @param   comparable A comparable which is used to search for the matching value.
     *          If the object matches multiple values, the returned value is not
     *          precisely specified.
     */
    // TODO Rename: binarySearch
    def find[T <: AnyRef](data: ArraySeq[T], comparable: Comparable[T]): Option[T] = {
        find(data)(comparable.compareTo)
    }

    // TODO Rename: binarySearch
    def find[T <: AnyRef](data: ArraySeq[T])(compareTo: T => Int): Option[T] = {
        @tailrec @inline def find(low: Int, high: Int): Option[T] = {
            if (high < low)
                return None;

            val mid = (low + high) / 2 // <= will never overflow...(by constraint...)
            val e = data(mid)
            val eComparison = compareTo(e)
            if (eComparison == 0) {
                Some(e)
            } else if (eComparison < 0) {
                find(mid + 1, high)
            } else {
                find(low, mid - 1)
            }
        }

        find(0, data.length - 1)
    }
}

package control {

    /**
     * Implementation of the macros.
     *
     * @author Michael Eichberg
     */
    private object ControlAbstractionsImplementation {

        def foreachNonNullValue[T <: AnyRef](
            a: Expr[Array[T]]
        )(
            f: Expr[(Int, T) => Unit]
        )(using Quotes, Type[T]): Expr[Unit] = {
            '{
                val array = ${ a } // evaluate only once!
                val arrayLength = array.length
                var i = 0
                while (i < arrayLength) {
                    val arrayEntry = array(i)
                    if (arrayEntry ne null) ${ f }(i, arrayEntry)
                    i += 1
                }
            }
        }

        def foreachWithIndex[T <: AnyRef](
            a: Expr[Array[T]]
        )(
            f: Expr[(T, Int) => Unit]
        )(using Quotes, Type[T]): Expr[Unit] = {
            '{
                val array = ${ a } // evaluate only once!
                val arrayLength = array.length
                var i = 0
                while (i < arrayLength) {
                    val arrayEntry = array(i)
                    ${ f }(arrayEntry, i)
                    i += 1
                }
            }
        }

        def forFirstN[T <: AnyRef](
            l: Expr[List[T]],
            n: Expr[Int]
        )(
            f: Expr[T => Unit]
        )(using Quotes, Type[T]): Expr[Unit] = {
            '{
                var remainingList = ${ l }
                val max = ${ n }
                var i = 0
                while (i < max) {
                    val head = remainingList.head
                    remainingList = remainingList.tail
                    ${ f }(head)
                    i += 1
                }
            }
        }

        /*def fillRefArray[T <: AnyRef: ClassTag](
            times: Expr[Int]
        )(
            f: Expr[T]
        )(using Quotes, Type[T]): Expr[ArraySeq[T]] = {
            '{
                val size = ${times} // => times is evaluated only once
                if (size == 0) {
                    ArraySeq.empty[T]
                } else {
                    val array = new Array[AnyRef](size)
                    var i = 0
                    while (i < size) {
                        val value = ${f} // => we evaluate f the given number of times
                        array(i) = value
                        i += 1
                    }
                    ArraySeq.unsafeWrapArray[T](array.asInstanceOf[Array[T]])
                }
            }
        }*/

        def fillIntArray(times: Expr[Int])(f: Expr[Int])(using Quotes): Expr[ArraySeq[Int]] = {
            '{
                val size = ${ times } // => times is evaluated only once
                if (size == 0) {
                    ArraySeq.empty[Int]
                } else {
                    val array = new Array[Int](size)
                    var i = 0
                    while (i < size) {
                        val value = ${ f } // => we evaluate f the given number of times
                        array(i) = value
                        i += 1
                    }
                    ArraySeq.unsafeWrapArray(array)
                }
            }
        }

        def fillArrayOfInt(times: Expr[Int])(f: Expr[Int])(using Quotes): Expr[Array[Int]] = {
            '{
                val size = ${ times } // => times is evaluated only once
                if (size == 0) {
                    Array.empty
                } else {
                    val array = new Array[Int](size)
                    var i = 0
                    while (i < size) {
                        val value = ${ f } // => we evaluate f the given number of times
                        array(i) = value
                        i += 1
                    }
                    array
                }
            }
        }

        def iterateTo(
            from: Expr[Int],
            to:   Expr[Int]
        )(
            f: Expr[(Int) => Unit]
        )(using Quotes): Expr[Unit] = {
            '{
                var i = ${ from }
                val max = ${ to } // => to is evaluated only once
                while (i <= max) {
                    ${ f }(i) // => we evaluate f the given number of times
                    i += 1
                }
            }
        }

        def iterateUntil(
            from:  Expr[Int],
            until: Expr[Int]
        )(
            f: Expr[(Int) => Unit]
        )(using Quotes): Expr[Unit] = {
            '{
                var i = ${ from }
                val max = ${ until } // => until is evaluated only once
                while (i < max) {
                    ${ f }(i) // => we evaluate f the given number of times
                    i += 1
                }
            }
        }

        def repeat(times: Expr[Int])(f: Expr[Unit])(using Quotes): Expr[Unit] = {
            '{
                var i = ${ times }
                while (i > 0) {
                    ${ f } // => we evaluate f the given number of times
                    i -= 1
                }
            }
        }
    }
}
