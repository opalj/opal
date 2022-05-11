/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long

object ScalaLongSetEval extends LongSetEval {

    def main(args: Array[String]): Unit = {
        eval[Set[Long]](
            () => Set.empty[Long],
            (set: Set[Long]) => set.size,
            (set: Set[Long]) => set.+,
            (set: Set[Long]) => set.contains,
            (set: Set[Long]) => set.foreach _,
            (set: Set[Long]) => set.foldLeft[Long]
        )
    }

}