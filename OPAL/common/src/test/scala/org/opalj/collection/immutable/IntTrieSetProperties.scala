/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith

//import scala.collection.immutable.SortedSet

import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `IntTrieSet` by creating a standard Scala Set and comparing
 * the results of the respective functions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object IntTrieSetProperties extends Properties("IntTrieSet") {

    val smallListsGen = for { m ← Gen.listOfN(8, Arbitrary.arbitrary[Int]) } yield (m)

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create singleton IntTrieSet") = forAll { v: Int ⇒
        val factoryITS = IntTrieSet1(v)
        val directITS = new IntTrieSet1(v)
        val viaEmptyITS = EmptyIntTrieSet + v
        factoryITS.size == 1 &&
            factoryITS.head == v &&
            factoryITS == directITS &&
            directITS == viaEmptyITS &&
            factoryITS.isSingletonSet
    }

    property("create IntTrieSet from Set (i.e., no duplicates)") = forAll { s: Set[Int] ⇒
        val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        (its.size == s.size) :| s"size mismatch ${its.size} vs. ${s.size}" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.hasMultipleElements == (s.size > 1)) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toSet.toList.sorted == s.toList.sorted) :| "same content"
    }

    property("create IntTrieSet from List (i.e., with duplicates)") = forAll { l: List[Int] ⇒
        val its = l.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        val lWithoutDuplicates = l.toSet.toList
        (its.size == lWithoutDuplicates.size) :| s"size mismatch ${its.size} vs. ${lWithoutDuplicates.size}" &&
            its.iterator.toSet.toList.sorted == lWithoutDuplicates.sorted
    }

    property("head") = forAll { s: Set[Int] ⇒
        s.nonEmpty ==> {
            val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
            s.contains(its.head)
        }
    }

    property("mkString") = forAll { (s: Set[Int], pre: String, in: String, post: String) ⇒
        val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        val itsString = its.mkString(pre, in, post)
        val sString = its.iterator.mkString(pre, in, post)
        (itsString == sString) :| s"$itsString vs $sString"
    }

    /*

    property("foreach") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        var newS = Set.empty[Int]
        fl1.foreach(newS += _)
        s == newS
    }

    property("withFilter -> iterator (does not force evaluation)") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        var newS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        fl1.withFilter(_ >= 0).withFilter(_ <= 1000).iterator.toList == newS.toList.sorted
    }

    property("withFilter -> foreach (does not force evaluation)") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        var newS = Set.empty[Int]
        var newFLS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        fl1.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newFLS += _)
        newS == newFLS
    }

    property("withFilter -> size|empty|hasMultipleElements (does not force evaluation)") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        var newS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        val newFLS = fl1.withFilter(_ >= 0).withFilter(_ <= 1000)
        newS.size == newFLS.size &&
            newS.isEmpty == newFLS.isEmpty &&
            (newS.size >= 2) == newFLS.hasMultipleElements &&
            (newFLS.isEmpty || newS.min == newFLS.min && newS.max == newFLS.max)
    }

    property("map") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        val result = fl1.map(_ * 2 / 3)
        result.iterator.toList == s.map(_ * 2 / 3).toList.sorted &&
            result.isInstanceOf[IntArraySet]
    }

    property("map (identity)") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        (fl1.map(i ⇒ i) eq fl1)
    }

    property("flatMap") = forAll { s: Seq[Set[Int]] ⇒
        val fl1: IntArraySet = IntArraySetBuilder(s.flatten: _*).result
        val indicesSet = IntArraySetBuilder(s.toIndexedSeq.indices.toSet).result
        val result = indicesSet.flatMap(i ⇒ IntArraySetBuilder(s(i)).result)
        (fl1 == result) :| "construction independent results" &&
            (fl1.iterator.toList == s.flatten.toSet.toList.sorted) :| s"equal results (${fl1.iterator.toList} vs ${s.flatten.toList.sorted})"
    }

    property("-") = forAll { (s: Set[Int], v: Int) ⇒
        val fl1 = IntArraySetBuilder(s).result
        (s - v).toList.sorted == (fl1 - v).iterator.toList
    }

    property("+ (based on set)") = forAll { (s: Set[Int], v: Int) ⇒
        val fl1 = IntArraySetBuilder(s).result
        (s + v).toList.sorted == (fl1 + v).iterator.toList
    }

    property("+ (based on list)") = forAll { (l: List[Int], v: Int) ⇒
        val ias1 = IntArraySetBuilder(l: _*).result
        val s = l.toSet
        classify(s.size < l.size, "list with redundant values") {
            ias1.iterator.toList == s.toList.sorted
        }
    }

    property("subsetOf") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val fl2 = IntArraySetBuilder(s2).result
        s1.subsetOf(s2) == fl1.subsetOf(fl2)
    }

    property("iterator") = forAll { s1: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val ss1 = SortedSet.empty[Int] ++ s1
        ss1.iterator.toList == fl1.iterator.toList
    }

    property("apply") = forAll { s1: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val ss1 = (SortedSet.empty[Int] ++ s1).toList
        (0 until ss1.size).forall(i ⇒ fl1(i) == ss1(i))
    }

    property("equals") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val ias1 = IntArraySetBuilder(s1).result
        val ias2 = IntArraySetBuilder(s2).result
        val s1AndS2AreEqual = s1 == s2
        classify(s1AndS2AreEqual, s"s1 and s2 are equal") {
            (ias1 == ias2) == s1AndS2AreEqual
        }
    }

    property("hashCode") = forAll { s1: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val setHashCode = fl1.hashCode
        val arraysHashCode = java.util.Arrays.hashCode(s1.toList.sorted.toArray)
        (setHashCode == arraysHashCode) :| s"$setHashCode vs. $arraysHashCode ($fl1)"
    }

    property("toIntIterator") = forAll { s1: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val flIt = fl1.iterator
        val flIntIt = fl1.toIntIterator
        flIntIt.forall(i ⇒ flIt.next == i)
    }

    property("toChain") = forAll { s1: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val iasBasedChaing = fl1.toChain
        val sBasedChain = Chain(s1.toList.sorted: _*)
        (iasBasedChaing == sBasedChain) :| s"$iasBasedChaing vs. $sBasedChain ($s1)"
    }

    property("contains") = forAll { (s: Set[Int], v: Int) ⇒
        val fl1 = IntArraySetBuilder(s).result
        (s.contains(v) == fl1.contains(v)) :| "is contained in"
    }

    property("exists") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        s.exists(_ / 3 == 0) == fl1.exists(_ / 3 == 0)
    }

    property("foldLeft") = forAll { (s: Set[Int], v: String) ⇒
        val fl1 = IntArraySetBuilder(s).result
        (s).toList.sorted.foldLeft(v)(_ + _) == fl1.foldLeft(v)(_ + _)
    }

    property("forall") = forAll { s: Set[Int] ⇒
        val fl1 = IntArraySetBuilder(s).result
        s.forall(_ >= 0) == fl1.forall(_ >= 0)
    }

    property("++") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val fl1 = IntArraySetBuilder(s1).result
        val fl2 = IntArraySetBuilder(s2).result
        (s1 ++ s2).toList.sorted == (fl1 ++ fl2).iterator.toList
    }

    property("mkString") = forAll { (s: Set[Int], pre: String, in: String, post: String) ⇒
        val fl1 = IntArraySetBuilder(s).result
        s.toList.sorted.mkString(pre, in, post) == fl1.mkString(pre, in, post)
    }
*/
}
