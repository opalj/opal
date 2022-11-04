/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith

import scala.collection.immutable.SortedSet

import org.scalatestplus.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.propBoolean
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `IntArraySet` by creating standard Scala Set and comparing
 * the results of the respective functions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object IntArraySetProperties extends Properties("IntArraySet") {

    val smallListsGen: Gen[List[UByte]] = for { m <- Gen.listOfN(8, Arbitrary.arbitrary[Int]) } yield (m)

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create singleton IntArraySet") = forAll { s: Int =>
        val fl1 = IntArraySet(s)
        val fl2 = (new IntArraySetBuilder += s).result()
        val fl3 = IntArraySet1(s)
        s == fl1.head && fl1.head == fl2.head && fl2.head == fl3.head
    }

    property("create two values IntArraySet") = forAll { (s1: Int, s2: Int) =>
        val fl1 = IntArraySet(s1, s2)
        val fl2 = (new IntArraySetBuilder += s1 += s2).result()
        val fl3 = Set(s1, s2)
        (fl1.size == fl2.size) :| "fl1.size == fl2.size" &&
            (fl2.size == fl3.size) :| "fl2.size == fl3.size" &&
            fl3.contains(fl1.min) :| "fl3.contains(fl1.min)" &&
            fl3.contains(fl1.max) :| "fl3.contains(fl1.max)" &&
            fl1 == fl2
    }

    property("create three values IntArraySet") = forAll { (s1: Int, s2: Int, s3: Int) =>
        val fl1 = IntArraySet(s1, s2, s3)
        val fl2 = (new IntArraySetBuilder += s1 += s2 += s3).result()
        val fl3 = Set(s1, s2, s3)
        fl1.size == fl2.size && fl2.size == fl3.size &&
            (fl1.size < 3 || (fl3.contains(fl1(1)) && fl3.contains(fl1(2)) && fl3.contains(fl1(2))))
    }

    property("size|empty|nonEmpty|hasMultipleElements") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        s.isEmpty == fl1.isEmpty && fl1.isEmpty != fl1.nonEmpty &&
            s.size == fl1.size &&
            (s.size >= 2 && fl1.hasMultipleElements) || (s.size <= 1 && !fl1.hasMultipleElements)
    }

    property("min|max|head|last") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        s.isEmpty && fl1.isEmpty || {
            s.min == fl1.min && s.max == fl1.max && fl1.min == fl1.head && fl1.max == fl1.last
        }
    }

    property("foreach") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        var newS = Set.empty[Int]
        fl1.foreach(newS += _)
        s == newS
    }

    property("withFilter -> iterator (does not force evaluation)") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        var newS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        fl1.withFilter(_ >= 0).withFilter(_ <= 1000).iterator.toList == newS.toList.sorted
    }

    property("withFilter -> foreach (does not force evaluation)") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        var newS = Set.empty[Int]
        var newFLS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        fl1.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newFLS += _)
        newS == newFLS
    }

    property("withFilter -> size|empty|hasMultipleElements (does not force evaluation)") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        var newS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        val newFLS = fl1.withFilter(_ >= 0).withFilter(_ <= 1000)
        newS.size == newFLS.size &&
            newS.isEmpty == newFLS.isEmpty &&
            (newS.size >= 2) == newFLS.hasMultipleElements &&
            (newFLS.isEmpty || newS.min == newFLS.min && newS.max == newFLS.max)
    }

    property("map") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        val result = fl1.map(_ * 2 / 3)
        result.iterator.toList == s.map(_ * 2 / 3).toList.sorted &&
            result.isInstanceOf[IntArraySet]
    }

    property("map (identity)") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        fl1.map(i => i) eq fl1
    }

    property("flatMap") = forAll { s: Seq[Set[Int]] =>
        val fl1: IntArraySet = IntArraySetBuilder(s.flatten: _*).result()
        val indicesSet = IntArraySetBuilder(s.toIndexedSeq.indices.toSet).result()
        val result = indicesSet.flatMap(i => IntArraySetBuilder(s(i)).result())
        (fl1 == result) :| "construction independent results" &&
            (fl1.iterator.toList == s.flatten.toSet.toList.sorted) :| "results are equal"
    }

    property("-") = forAll { (s: Set[Int], v: Int) =>
        val fl1 = IntArraySetBuilder(s).result()
        (s - v).toList.sorted == (fl1 - v).iterator.toList
    }

    property("+ (based on set)") = forAll { (s: Set[Int], v: Int) =>
        val fl1 = IntArraySetBuilder(s).result()
        (s + v).toList.sorted == (fl1 + v).iterator.toList
    }

    property("+ (based on list)") = forAll { (l: List[Int], v: Int) =>
        val ias1 = IntArraySetBuilder(l: _*).result()
        val s = l.toSet
        classify(s.size < l.size, "list with redundant values") {
            ias1.iterator.toList == s.toList.sorted
        }
    }

    property("subsetOf") = forAll { (s1: Set[Int], s2: Set[Int]) =>
        val fl1 = IntArraySetBuilder(s1).result()
        val fl2 = IntArraySetBuilder(s2).result()
        s1.subsetOf(s2) == fl1.subsetOf(fl2)
    }

    property("iterator") = forAll { s1: Set[Int] =>
        val fl1 = IntArraySetBuilder(s1).result()
        val ss1 = SortedSet.empty[Int] ++ s1
        ss1.iterator.toList == fl1.iterator.toList
    }

    property("apply") = forAll { s1: Set[Int] =>
        val fl1 = IntArraySetBuilder(s1).result()
        val ss1 = (SortedSet.empty[Int] ++ s1).toList
        ss1.indices.forall(i => fl1(i) == ss1(i))
    }

    property("equals") = forAll { (s1: Set[Int], s2: Set[Int]) =>
        val ias1 = IntArraySetBuilder(s1).result()
        val ias2 = IntArraySetBuilder(s2).result()
        val s1AndS2AreEqual = s1 == s2
        classify(s1AndS2AreEqual, s"s1 and s2 are equal") {
            (ias1 == ias2) == s1AndS2AreEqual
        }
    }

    property("hashCode") = forAll { s1: Set[Int] =>
        val fl1 = IntArraySetBuilder(s1).result()
        val setHashCode = fl1.hashCode
        val arraysHashCode = java.util.Arrays.hashCode(s1.toList.sorted.toArray)
        (setHashCode == arraysHashCode) :| "hashCode equality"
    }

    property("intIterator") = forAll { s1: Set[Int] =>
        val fl1 = IntArraySetBuilder(s1).result()
        val flIt = fl1.iterator
        flIt.isInstanceOf[IntIterator]
    }

    property("toList") = forAll { s1: Set[Int] =>
        val fl1 = IntArraySetBuilder(s1).result()
        val iasBasedChaing = fl1.toList
        val sBasedChain = List(s1.toList.sorted: _*)
        (iasBasedChaing == sBasedChain) :| "equality of chains"
    }

    property("contains") = forAll { (s: Set[Int], v: Int) =>
        val fl1 = IntArraySetBuilder(s).result()
        (s.contains(v) == fl1.contains(v)) :| "is contained in"
    }

    property("exists") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        s.exists(_ / 3 == 0) == fl1.exists(_ / 3 == 0)
    }

    property("foldLeft") = forAll { (s: Set[Int], v: String) =>
        val fl1 = IntArraySetBuilder(s).result()
        s.toList.sorted.foldLeft(v)(_ + _) == fl1.foldLeft(v)(_ + _)
    }

    property("forall") = forAll { s: Set[Int] =>
        val fl1 = IntArraySetBuilder(s).result()
        s.forall(_ >= 0) == fl1.forall(_ >= 0)
    }

    property("++") = forAll { (s1: Set[Int], s2: Set[Int]) =>
        val fl1 = IntArraySetBuilder(s1).result()
        val fl2 = IntArraySetBuilder(s2).result()
        (s1 ++ s2).toList.sorted == (fl1 ++ fl2).iterator.toList
    }

    property("mkString") = forAll { (s: Set[Int], pre: String, in: String, post: String) =>
        val fl1 = IntArraySetBuilder(s).result()
        s.toList.sorted.mkString(pre, in, post) == fl1.mkString(pre, in, post)
    }

}
