/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set 
package long

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
//import org.scalacheck.Gen
//import org.scalacheck.Arbitrary
import org.scalatest.Matchers
import org.scalatest.FunSpec

import org.opalj.util.PerformanceEvaluation

/**
 * Generic Tests for `LongSet`s.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
abstract class LongSetProperties(typeName : String) extends Properties(typeName) {
    
    def empty : LongSet

    property("+, size, foreach") = forAll { sLongSet: Set[Long] ⇒
        val oLongSet =  sLongSet.foldLeft(empty)(_ + _)
        var newSLongSet = Set.empty[Long]
        oLongSet foreach { newSLongSet += _ } 
        sLongSet == newSLongSet && 
            oLongSet.size == sLongSet.size &&
            (oLongSet.size == 0 && oLongSet.isEmpty || oLongSet.size != 0 && oLongSet.nonEmpty) &&
            oLongSet.isSingletonSet == (oLongSet.size == 1) &&
            oLongSet.isEmpty != oLongSet.nonEmpty
    }

    property("forall and contains") = forAll { sLongSet: Set[Long] ⇒
        val sLongList = sLongSet.toList
        val oLongSet =  sLongList.foldLeft(empty)(_ + _)
        oLongSet.forall(sLongSet.contains) && 
        sLongList.forall(oLongSet.contains) 
    }

    property("foldLeft") = forAll { sLongSet: Set[Long] ⇒
        val sLongList = sLongSet.toList
        val oLongSet =  sLongList.foldLeft(empty)(_ + _)
        oLongSet.foldLeft(0L)( _ + _ ) == sLongList.foldLeft(0L)(_ + _)
    }

    property("iterator") = forAll { sLongSet: Set[Long] ⇒
        val oLongSet =  sLongSet.foldLeft(empty)(_ + _)
        var newSLongSet = Set.empty[Long]
        oLongSet.iterator foreach { newSLongSet += _ } 
        sLongSet == newSLongSet
    }

    property("+: only adding values which are in the set doesn't change the set") = forAll { sLongSet: Set[Long] ⇒
        val oLongSet =  sLongSet.foldLeft(empty)(_ + _)
        val updatedOLongSet = sLongSet.foldLeft(oLongSet)(_ + _)
        oLongSet eq updatedOLongSet
    }

    property("equals and hashCode: two sets which are definitively equal (the same values are added in the same order) equal have to have the same hash code") = forAll { sLongSet: Set[Long] ⇒
        val sLongList = sLongSet.toList
        val oLongSet1 =  sLongList.foldLeft(empty)(_ + _)
        val oLongSet2 =  sLongList.foldLeft(empty)(_ + _)

        (oLongSet1 == oLongSet2) :| "equals" && 
        (oLongSet1.hashCode == oLongSet2.hashCode) :| "hashCode"
    }
}
