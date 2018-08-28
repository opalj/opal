/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

case class IntAnyRefPair[+T](_1: Int, _2: T) extends Product2[Int, T] {

    def key: Int = _1
    def value: T = _2
    def first: Int = _1
    def second: T = _2

    def head: Int = _1
    def rest: T = _2
}
