/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * A simple pairing of an int value and a reference value.
 *
 * @param _1 The first value.
 * @param _2 The second value.
 * @tparam T The type of the reference value.
 *
 * @author Michael Eichberg
 */
final case class IntRefPair[+T](_1: Int, _2: T) extends Product2[Int, T] {

    def first: Int = _1
    def second: T = _2

    def key: Int = _1
    def value: T = _2

    def head: Int = _1
    def rest: T = _2
}

/**
 * A simple pairing of a reference value and an int value.
 *
 * @param _1 The first value.
 * @param _2 The second value.
 * @tparam T The type of the reference value.
 *
 * @author Michael Eichberg
 */
final case class RefIntPair[+T](_1: T, _2: Int) extends Product2[T, Int] {

    def first: T = _1
    def second: Int = _2

    def key: T = _1
    def value: Int = _2

    def head: T = _1
    def rest: Int = _2
}