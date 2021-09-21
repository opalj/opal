/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LongTrieSetTest extends LongSetTest {

    def empty(): LongSet = LongTrieSet.empty

    // The following methods should be implemented using the most fitting
    // factory methods.
    def create(v1: Long): LongSet = LongTrieSet(v1)
    def create(v1: Long, v2: Long): LongSet = LongTrieSet(v1, v2)
    def create(v1: Long, v2: Long, v3: Long): LongSet = LongTrieSet(v1, v2, v3)
    def create(v1: Long, v2: Long, v3: Long, v4: Long): LongSet = LongTrieSet(v1, v2, v3) + (v4)

}
