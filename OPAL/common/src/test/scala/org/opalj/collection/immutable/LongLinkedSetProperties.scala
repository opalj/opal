/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.propBoolean

/**
 * Generic Tests for `LongSet`s.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
abstract class LongLinkedSetProperties(typeName: String) extends LongSetProperties(typeName) {

    def empty(): LongLinkedSet

    property("head") = forAll { sLongSet: Set[Long] =>
        sLongSet.nonEmpty ==> {
            val sLongList = sLongSet.toList
            val oLongSet = sLongList.reverse.foldLeft(empty())(_ + _)
            oLongSet.head == sLongList.head
        }
    }

    property("foreach in reverse insertion order (newest first)") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongSet = sLongList.foldLeft(empty())(_ + _)
        var newSLongList = List.empty[Long]
        oLongSet.foreach(newSLongList ::= _)
        sLongList == newSLongList
    }

    property("forFirstN") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongSet = sLongList.reverse.foldLeft(empty())(_ + _)
        (0 to sLongSet.size).forall { initSize =>
            var newInitSLongList = List.empty[Long]
            oLongSet.forFirstN(initSize)(newInitSLongList ::= _)
            sLongList.take(initSize) == newInitSLongList.reverse
        }
    }
}
