/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

/**
 * A simple pairing of a long value and a reference value.
 *
 * @param _1 The first value.
 * @param _2 The second value.
 * @tparam T The type of the reference value.
 *
 * @author Michael Eichberg
 */
final case class LongRefPair[+T](_1: Long, _2: T) {

    def first: Long = _1
    def second: T = _2

    def key: Long = _1
    def value: T = _2

    def head: Long = _1
    def rest: T = _2
}
