/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long


object ScalaLongSetEval extends LongSetEval {

    /*
    def eval[T](
        empty:    () ⇒ T,
          size:     (T) ⇒ Int,
        add:      (T) ⇒ Long ⇒ T,
        contains: (T) ⇒ Long ⇒ Boolean
    )
    */

    def main (args : Array[String] ) : Unit = {
            eval[Set[Long]](
                () => Set.empty[Long],
                (set : Set[Long]) => set.size,
                (set : Set[Long]) => set.+,
                (set : Set[Long]) => set.contains,
            )
    }

}