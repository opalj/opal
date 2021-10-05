/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

object LongTrieSetWithListEval extends LongSetEval {

    def main(args: Array[String]): Unit = {
        eval[LongTrieSetWithList](
            () => LongTrieSetWithList.empty,
            (set: LongTrieSetWithList) => set.size,
            (set: LongTrieSetWithList) => set.+,
            (set: LongTrieSetWithList) => set.contains,
            (set: LongTrieSetWithList) => set.foreach,
            (set: LongTrieSetWithList) => set.foldLeft[Long]
        )
    }

}