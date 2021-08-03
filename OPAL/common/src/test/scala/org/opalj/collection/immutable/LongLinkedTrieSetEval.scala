/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

object LongLinkedTrieSetEval extends LongSetEval {

    def main(args: Array[String]): Unit = {
        eval[LongLinkedTrieSet](
            () => LongLinkedTrieSet.empty,
            (set: LongLinkedTrieSet) => set.size,
            (set: LongLinkedTrieSet) => set.+,
            (set: LongLinkedTrieSet) => set.contains,
            (set: LongLinkedTrieSet) => set.foreach,
            (set: LongLinkedTrieSet) => set.foldLeft[Long]
        )
    }

}