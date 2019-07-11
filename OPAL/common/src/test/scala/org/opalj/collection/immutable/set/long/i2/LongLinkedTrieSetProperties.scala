/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long
package i2

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
object LongLinkedTrieSetProperties extends LongSetProperties("LongLinkedTrieSetProperties") {

    def empty(): LongSet = LongLinkedTrieSet.empty

}
