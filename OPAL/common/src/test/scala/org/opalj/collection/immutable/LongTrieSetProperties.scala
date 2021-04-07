/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
object LongTrieSetProperties extends LongSetProperties("LongTrieSetProperties") {

    def empty(): LongSet = LongTrieSet.empty

}
