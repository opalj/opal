/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.propBoolean

@RunWith(classOf[JUnitRunner])
object Long2ListProperties extends Properties("Long2List") {

    def empty(): Long2List = Long2List.empty

    property("+:, size, isSingletonList, isEmpty, foreach") = forAll { sLongSet: Set[Long] =>
        val oLongList = sLongSet.foldLeft(empty())((c, n) => n +: c)
        var newSLongSet = Set.empty[Long]
        oLongList foreach { newSLongSet += _ }
        (sLongSet == newSLongSet) :| "foreach" &&
            (oLongList.isSingletonList == (oLongList.foldLeft(0)((c, n) => c + 1) == 1)) :| "isSingletonList and foldLeft" &&
            (oLongList.isEmpty != oLongList.nonEmpty) :| "nonEmpty and isEmpty"
    }

    property("forall") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongList = sLongList.foldLeft(empty())((c, n) => n +: c)
        oLongList.forall(sLongSet.contains)
    }

    property("forFirstN") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongList = sLongList.reverse.foldLeft(empty())((c, n) => n +: c)
        (0 to sLongSet.size).forall { i =>
            var sum = 0L
            oLongList.forFirstN(i)(v => sum += v)
            if (sum != sLongList.take(i).sum) {
                println(s"sum of first $i elements of $oLongList is $sum")
                false
            } else {
                true
            }
        }
    }

    property("foldLeft") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongList = sLongList.foldLeft(empty())((c, n) => n +: c)
        oLongList.foldLeft(0L)(_ + _) == sLongList.foldLeft(0L)(_ + _)
    }

    property("iterator") = forAll { sLongSet: Set[Long] =>
        val oLongList = sLongSet.foldLeft(empty())((c, n) => n +: c)
        var newSLongSet = Set.empty[Long]
        oLongList.iterator foreach { newSLongSet += _ }
        sLongSet == newSLongSet
    }

    property("equals and hashCode: two sets which are definitively equal (the same values are added in the same order) equal have to have the same hash code") = forAll { sLongSet: Set[Long] =>
        val sLongListSet = sLongSet.toList
        val oLongList1 = sLongListSet.foldLeft(empty())((c, n) => n +: c)
        val oLongList2 = sLongListSet.foldLeft(empty())((c, n) => n +: c)

        (oLongList1 == oLongList2) :| s"equals:\n$oLongList1\n\tvs.\n$oLongList2" &&
            (oLongList1.hashCode == oLongList2.hashCode) :| "hashCode"
    }

    property("equals: two sets which are definitively not equal should not be equal") = forAll { sLongSet: Set[Long] =>
        sLongSet.nonEmpty ==> {
            val sLongListSet = sLongSet.toList
            val oLongList1 = sLongListSet.foldLeft(empty())((c, n) => n +: c)
            val oLongList2 = sLongListSet.tail.foldLeft(empty())((c, n) => n +: c)

            (oLongList1 != oLongList2) :| s"equals:\n$oLongList1\n\tvs.\n$oLongList2"
        }
    }
}
