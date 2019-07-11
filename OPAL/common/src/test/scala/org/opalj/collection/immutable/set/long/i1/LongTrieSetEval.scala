/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long
package i1


object LongTrieSetEval extends LongSetEval {

    /*
    def eval[T](
        empty:    () ⇒ T,
        add:      (T) ⇒ Long ⇒ T,
        contains: (T) ⇒ Long ⇒ Boolean
    )
    */

    def main (args : Array[String] ) : Unit = {
            eval[LongTrieSet](
                () => LongTrieSet.empty,
                (set : LongTrieSet) => set.size,
                (set : LongTrieSet) => set.+,
                (set : LongTrieSet) => set.contains,
            )
    }

}