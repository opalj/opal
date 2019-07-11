/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long

/**
 * A set of long values.
 *
 * @author Michael Eichberg
 */
trait LongLinkedSet extends LongSet {

    type ThisSet <: LongLinkedSet

    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit

}
