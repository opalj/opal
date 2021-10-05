/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

/**
 * An immutable pair of int values.
 *
 * @author Michael Eichberg
 */
final case class IntIntPair(_1: Int, _2: Int) {

    def first: Int = _1
    def second: Int = _2

    def key: Int = _1
    def value: Int = _2

    def foreach(f: Int => Unit): Unit = { f(_1); f(_2) }

    def incrementKey(increment: Int): IntIntPair = {
        new IntIntPair(_1 + increment, _2)
    }

    def incrementValue(increment: Int): IntIntPair = {
        new IntIntPair(_1, _2 + increment)
    }
}

