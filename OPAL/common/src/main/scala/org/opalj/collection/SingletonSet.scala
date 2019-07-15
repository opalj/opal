/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection

import scala.collection.Set

/**
 * Facilitates the matching of a Scala collection `Set` that contains a single value.
 *
 * @author Michael Eichberg
 */
object SingletonSet {

    def unapply[T](s: Set[T]): Option[T] = if (s.size == 1) s.headOption else None

    def unapply(s: IntSet[_]): Option[Int] = if (s.size == 1) Some(s.head) else None

}
