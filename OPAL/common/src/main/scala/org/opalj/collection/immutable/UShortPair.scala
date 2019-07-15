/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * A representation of a pair of unsigned short values.
 *
 * @example
 * {{{
 * scala> val p = org.opalj.collection.immutable.UShortPair(2323,332)
 * p: org.opalj.collection.immutable.UShortPair = UShortPair(2323,332)
 * }}}
 *
 * @author Michael Eichberg
 */
final class UShortPair private (val pair: Int) extends AnyVal {

    def _1: UShort = pair & UShort.MaxValue
    def key: UShort = _1
    def minor: UShort = _1

    def _2: UShort = pair >>> 16
    def value: UShort = _2
    def major: UShort = _2

    override def toString: String = s"UShortPair($minor,$major)"
}
/**
 * Factory to create `UShortPair` objects.
 */
object UShortPair {

    def apply(a: UShort, b: UShort): UShortPair = {
        assert(a >= UShort.MinValue && a <= UShort.MaxValue)
        assert(b >= UShort.MinValue && b <= UShort.MaxValue)

        new UShortPair(a | b << 16)
    }
}
