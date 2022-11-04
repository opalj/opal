/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.propBoolean

/**
 * Generic Tests for `LongSet`s.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
abstract class LongSetProperties(typeName: String) extends Properties(typeName) {

    def empty(): LongSet

    property("+, size, isSingletonSet, isEmpty, foreach") = forAll { sLongSet: Set[Long] =>
        val oLongSet = sLongSet.foldLeft(empty())(_ + _)
        var newSLongSet = Set.empty[Long]
        oLongSet foreach { newSLongSet += _ }
        sLongSet == newSLongSet &&
            oLongSet.size == sLongSet.size &&
            (oLongSet.size == 0 && oLongSet.isEmpty || oLongSet.size != 0 && oLongSet.nonEmpty) &&
            oLongSet.isSingletonSet == (oLongSet.size == 1) &&
            oLongSet.isEmpty != oLongSet.nonEmpty
    }

    property("forall and contains") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongSet = sLongList.foldLeft(empty())(_ + _)
        oLongSet.forall(sLongSet.contains) &&
            sLongList.forall(oLongSet.contains)
    }

    property("foldLeft") = forAll { sLongSet: Set[Long] =>
        val sLongList = sLongSet.toList
        val oLongSet = sLongList.foldLeft(empty())(_ + _)
        oLongSet.foldLeft(0L)(_ + _) == sLongList.foldLeft(0L)(_ + _)
    }

    property("iterator") = forAll { sLongSet: Set[Long] =>
        val oLongSet = sLongSet.foldLeft(empty())(_ + _)
        var newSLongSet = Set.empty[Long]
        oLongSet.iterator foreach { newSLongSet += _ }
        sLongSet == newSLongSet
    }

    property("+: only adding values which are in the set doesn't change the set") = forAll { sLongSet: Set[Long] =>
        val oLongSet = sLongSet.foldLeft(empty())(_ + _)
        val updatedOLongSet = sLongSet.foldLeft(oLongSet)(_ + _)
        oLongSet eq updatedOLongSet
    }

    property("equals and hashCode: two sets which are definitively equal (the same values are added in the same order) equal have to have the same hash code") = forAll { sLongSet: Set[Long] =>
        val sLongListSet = sLongSet.toList
        val oLongSet1 = sLongListSet.foldLeft(empty())(_ + _)
        val oLongSet2 = sLongListSet.foldLeft(empty())(_ + _)

        (oLongSet1 == oLongSet2) :| s"equals:\n$oLongSet1\n\tvs.\n$oLongSet2" &&
            (oLongSet1.hashCode == oLongSet2.hashCode) :| "hashCode"
    }

    property("equals and hashCode: two sets which are definitively not equal should not be equal") = forAll { sLongSet: Set[Long] =>
        sLongSet.nonEmpty ==> {
            val sLongListSet = sLongSet.toList
            val oLongSet1 = sLongListSet.foldLeft(empty())(_ + _)
            val oLongSet2 = sLongListSet.tail.foldLeft(empty())(_ + _)

            (oLongSet1 != oLongSet2) :| s"equals:\n$oLongSet1\n\tvs.\n$oLongSet2"
        }
    }
}
