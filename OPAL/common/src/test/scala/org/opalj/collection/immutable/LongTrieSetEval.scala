/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

object LongTrieSetEval extends LongSetEval {

    def main(args: Array[String]): Unit = {
        eval[LongTrieSet](
            () => LongTrieSet.empty,
            (set: LongTrieSet) => set.size,
            (set: LongTrieSet) => set.+,
            (set: LongTrieSet) => set.contains,
            (set: LongTrieSet) => set.foreach,
            (set: LongTrieSet) => set.foldLeft[Long]
        )
    }

}