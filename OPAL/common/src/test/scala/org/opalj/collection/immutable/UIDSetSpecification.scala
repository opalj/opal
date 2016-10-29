/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import scala.language.implicitConversions

import org.junit.runner.RunWith
import scala.collection.immutable.SortedSet
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.{forAll, classify, BooleanOperators}
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `UIDSets` by creating standard Sets and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object UIDSetSpecification extends Properties("UIDSet") {

    case class SUID(id: Int) extends UID
    type SUIDSet = UIDSet[SUID]
    val EmptySUIDSet: SUIDSet = UIDSet0

    implicit def intToSUID(i: Int): SUID = SUID(i)
    implicit def toSUIDSet(l: Traversable[Int]): UIDSet[SUID] = {
        val ls: Traversable[SUID] = l.map(i ⇒ SUID(i))
        UIDSet0 ++ ls
    }
    def isSorted(s: SUIDSet): Boolean = {
        s.isEmpty || {
            var lastId = s.head.id
            s.tail.toIterator.foreach { suid ⇒
                if (suid.id <= lastId)
                    return false;
                lastId = suid.id

            }
            true
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                             G E N E R A T O R S

    val intSetGen: Gen[Set[Int]] = Gen.containerOf[Set, Int](Gen.posNum[Int])

    val oneToOneHunderdGen = Gen.choose(0, 100)

    val veryLargeListGen = for {
        i ← Gen.choose(30000, 100000)
        s ← Gen.containerOfN[List, Int](i, Arbitrary.arbitrary[Int])
    } yield (s, i)

    val largeSetGen = for {
        i ← Gen.choose(20, 100)
        s ← Gen.containerOfN[Set, Int](i, Arbitrary.arbitrary[Int])
    } yield (s, i)

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create singleton set") = forAll { i: Int ⇒
        val s = SUID(i)
        val fl1 = UIDSet[SUID](i)
        val fl2 = (UIDSet.newBuilder[SUID] += i).result
        val fl3 = (UIDSet0: UIDSet[SUID]) + s
        (fl1.head == s) :| "apply" && (fl2.head == s) :| "builder" && (fl3.head == s) :| "+"
    }

    property("singleton set when the set contains only one value after transformation") = forAll { (i: Int, j: Int) ⇒
        i != j ==> {
            UIDSet[SUID](i, j).tail.isSingletonSet :| "tail" &&
                (UIDSet[SUID](i) + SUID(j)).filter(_ == SUID(i)).isSingletonSet :| "filter first value" &&
                (UIDSet[SUID](i) + SUID(j)).filter(_ == SUID(j)).isSingletonSet :| "filter second value"
        }
    }

    property("create set(++)") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        (s.size == us.size) :| "size" &&
            s.forall(us.contains(_)) && us.forall(s.contains(_)) &&
            isSorted(us) :| "sorted"
    }

    property("create set(+)") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        var us = EmptySUIDSet
        s.foreach { us += _ }
        s.size == us.size && s.forall(us.contains(_)) && us.forall(s.contains(_)) && isSorted(us)
    }

    property("create set given two values") = forAll { (a: Int, b: Int) ⇒
        val us = UIDSet(SUID(a), SUID(b))
        (a == b && us.size == 1) || (a != b && us.size == 2) && isSorted(us)
    }

    property("==|hashCode[AnyRef]") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val us1 = UIDSet.empty[SUID] ++ s1.map(SUID(_))
        val us2 = UIDSet.empty[SUID] ++ s2.map(SUID(_))
        classify(s1 == s2, "both sets are equal") {
            (s1 == s2) == (us1 == us2) && (us1 != us2 || us1.hashCode() == us2.hashCode())
        }
    }

    property("hasDefiniteSize") = forAll { l: List[Int] ⇒
        val fl = toSUIDSet(l)
        fl.hasDefiniteSize
    }

    property("isTraversableAgain") = forAll { l: List[Int] ⇒
        val fl = toSUIDSet(l)
        fl.isTraversableAgain
    }

    property("seq") = forAll { l: List[Int] ⇒
        val fl = toSUIDSet(l)
        (fl.seq eq fl)
    }

    // METHODS DEFINED BY UIDSet

    property("isSingletonSet") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        (s.size == 1) == us.isSingletonSet
    }

    property("WithFilter") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        def test(s: SUID): Boolean = s.id > 0
        s.withFilter(test).map((s: SUID) ⇒ s) == us.withFilter(test).map((s: SUID) ⇒ s)(UIDSet.canBuildSetFromUIDSet)
    }

    property("uid sets can be used in for loop") = forAll { orig: List[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        var newUs = EmptySUIDSet
        for {
            s ← us
            if s != SUID(0)
        } {
            newUs += s
        }
        newUs == us.filter(_ != SUID(0))
    }

    property("uid sets can be used in for comprehensions") = forAll { orig: List[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        val newUs: SUIDSet = for { suid ← us } yield { SUID(suid.id + 1) }
        newUs == us.map[SUID, SUIDSet](suid ⇒ SUID(suid.id + 1))
    }

    property("head|first") = forAll { l: List[Int] ⇒
        val fl = toSUIDSet(l)
        l.isEmpty || (fl.head.id == l.min && fl.head == fl.first)
    }

    property("last") = forAll { l: List[Int] ⇒
        val fl = toSUIDSet(l)
        l.isEmpty || (fl.last.id == l.max)
    }

    property("contains") = forAll { (s: Set[Int], i: Int) ⇒
        val us = toSUIDSet(s)
        us.contains(SUID(i)) == s.contains(i)
    }

    property("exists") = forAll { (s: Set[Int]) ⇒
        val us = toSUIDSet(s)
        us.exists(_.id % 2 == 0) == s.exists(_ % 2 == 0)
    }

    property("find") = forAll { (s: Set[Int]) ⇒
        def test(suid: SUID) = suid.id < 0
        val us = toSUIDSet(s)
        val ssuid = (SortedSet.empty[Int] ++ s).toList.map(SUID(_))
        val usFound = us.find(test)
        val ssFound = ssuid.find(test)
        usFound.isDefined == ssFound.isDefined &&
            (usFound.isEmpty || usFound.get.id < 0)

    }

    property("compare(subsets)") = forAll { (s: Set[Int]) ⇒
        val us = toSUIDSet(s)
        classify(s.isEmpty, "the set is empty") {
            (s.isEmpty && us.compare(UIDSet0) == EqualSets) || (
                (s.tail.inits.forall(init ⇒ toSUIDSet(init).compare(us) == StrictSubset)) &&
                (s.tail.inits.forall(init ⇒ us.compare(toSUIDSet(init)) == StrictSuperset))
            )
        }
    }

    property("compare(arbitrary sets)") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val us1 = toSUIDSet(s1)
        val us2 = toSUIDSet(s2)
        val us1CompareUs2 = us1.compare(us2)
        val us2CompareUs1 = us2.compare(us1)
        (s1 == s2 && us1CompareUs2 == us2CompareUs1 && us1CompareUs2 == EqualSets) ||
            (s1.subsetOf(s2) && us1CompareUs2 == StrictSubset && us2CompareUs1 == StrictSuperset) ||
            (s2.subsetOf(s1) && us2CompareUs1 == StrictSubset && us1CompareUs2 == StrictSuperset) ||
            (us1CompareUs2 == us2CompareUs1 && us1CompareUs2 == UncomparableSets)
    }

    property("tail") = forAll { (orig: Set[Int]) ⇒
        orig.nonEmpty ==> {
            val s = SortedSet.empty ++ orig
            val us = toSUIDSet(s).tail
            val tailTest = (toSUIDSet(s.tail) == us)
            if (!tailTest) println(toSUIDSet(s.tail))
            if (!tailTest) println(us)
            tailTest :| "tail" &&
                isSorted(us) :| "isSorted"
        }
    }

    property("filter") = forAll { (s: Set[Int], i: Int) ⇒
        def test(s: SUID) = s.id < 0
        val us = toSUIDSet(s).filter(test)
        toSUIDSet(s.filter(i ⇒ test(SUID(i)))) == us && isSorted(us)
    }

    property("filterNot") = forAll { (s: Set[Int], i: Int) ⇒
        def test(s: SUID) = s.id < 0
        val us = toSUIDSet(s).filterNot(test)
        toSUIDSet(s.filterNot(i ⇒ test(SUID(i)))) == us && isSorted(us)
    }

    property("intersect") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val us1 = toSUIDSet(s1)
        val us2 = toSUIDSet(s2)
        val us = toSUIDSet(s1 intersect s2)
        us == (us1 intersect us2)
    }

    property("copyToArray") = forAll(intSetGen, oneToOneHunderdGen) { (orig: Set[Int], size: Int) ⇒
        (size >= 0 && size < 100) ==> {
            val s = SortedSet.empty ++ orig
            val sa = new Array[SUID](size)
            val usa = new Array[SUID](size)
            val us = toSUIDSet(s)
            s.copyToArray(sa, 4, 4) == us.copyToArray(usa, 4, 4)
        }
    }

    property("foldLeft") = forAll { (s: Set[Int], i: Int) ⇒
        toSUIDSet(s).foldLeft(0)(_ + _.id * 2) == s.foldLeft(0)(_ + _.id * 2)
    }

    property("map") = forAll { (orig: Set[Int]) ⇒
        val s = orig.map(SUID(_))
        def f(s: SUID): SUID = SUID(s.id % 5)
        val us: SUIDSet = toSUIDSet(orig).map(f)
        EmptySUIDSet ++ s.map(f) == us && isSorted(us)
    }

    property("toStream") = forAll { (orig: Set[Int]) ⇒
        val s = orig.map(SUID(_))
        val us = toSUIDSet(orig)
        s.toStream.toSet == us.toStream.toSet
    }

    property("toSeq") = forAll { (orig: Set[Int]) ⇒
        val s = orig.toList.sorted.map(SUID(_))
        val us = toSUIDSet(orig)
        s.toSeq == us.toSeq
    }

    property("toTraversable") = forAll { (orig: Set[Int]) ⇒
        val s = orig.toList.sorted.map(SUID(_))
        val us = toSUIDSet(orig)
        s.toTraversable.toList == us.toTraversable.toList
    }

    property("toString") = forAll { (orig: Set[Int]) ⇒
        val s = (SortedSet.empty[Int] ++ orig).toList.map(SUID(_))
        val us = toSUIDSet(orig)
        us.toString == s.mkString("UIDSet(", ", ", ")")
    }
    /*
    property("can handle large sets") = forAll(largeSetGen) { (v) ⇒
        val (orig: Set[Int], i) = v
        val us = toSUIDSet(orig)
        val usTransformed =
            (us.
                map[SUID, UIDSet[SUID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100)).
                tail

        val s = (SortedSet.empty[UID](UID.UIDBasedOrdering) ++ orig.map(SUID(_)))
        val sTransformed =
            (s.
                map[UID, SortedSet[UID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100)).
                tail
        classify(orig.size > 20, "original set is large") {
            classify(sTransformed.size > 20, "transformed set is large") {
                usTransformed.forall(sTransformed.contains) :| "us <= s" &&
                    sTransformed.forall(usTransformed.contains) :| "s <= us"
            }
        }
    }

    property("can handle very large sets") = forAll(veryLargeListGen) { (v) ⇒
        val (orig: List[Int], i) = v
        val us = toSUIDSet(orig)
        val usTransformed =
            //time{
            (us.
                map[SUID, UIDSet[SUID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100)).
                tail
        //}{t => println(t.toSeconds)}

        //            time{
        //            (us.
        //                map[SUID, UIDSet[SUID]] { e ⇒ SUID(e.id % i) })
        //            }{t => println(t.toSeconds)}

        val s = (SortedSet.empty[UID](UID.UIDBasedOrdering) ++ orig.map(SUID(_)))
        val sTransformed =
            //            time{
            (s.
                map[UID, SortedSet[UID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100)).
                tail
        //            }{t => println(t.toSeconds)}
        classify(s.size > 10000, s"original set is large") {
            classify(sTransformed.size > 10000, "transformed set is large") {
                usTransformed.forall(sTransformed.contains) :| "us <= s" &&
                    sTransformed.forall(usTransformed.contains) :| "s <= us"
            }
        }
    }
	*/

}
