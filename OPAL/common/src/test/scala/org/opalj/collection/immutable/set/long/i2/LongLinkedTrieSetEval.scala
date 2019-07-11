/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long
package i2


object LongLinkedTrieSetEval extends LongSetEval {

    def main (args : Array[String] ) : Unit = {
            eval[LongLinkedTrieSet](
                () => LongLinkedTrieSet.empty,
                (set : LongLinkedTrieSet) => set.size,
                (set : LongLinkedTrieSet) => set.+,
                (set : LongLinkedTrieSet) => set.contains,
            )
    }

}