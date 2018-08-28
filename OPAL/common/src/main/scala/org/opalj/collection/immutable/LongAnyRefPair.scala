/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

case class LongAnyRefPair[+T](_1: Long, _2: T) extends Product2[Long, T] {

    def key: Long = _1

    def value: T = _2

}
