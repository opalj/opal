/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
object LongLinkedTrieSetProperties extends LongLinkedSetProperties("LongLinkedTrieSetProperties") {

    def empty(): LongLinkedSet = LongLinkedTrieSet.empty

}
