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

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `UIDSets` by creating standard Sets and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object UIDSetProperties extends Properties("UIDSet") {

    case class SUID(id: Int) extends UID
    type SUIDSet = UIDSet[SUID]
    val EmptyUIDSet: SUIDSet = UIDSet.empty[SUID]

    implicit def intToSUID(i: Int): SUID = SUID(i)
    implicit def toSUIDSet(l: Traversable[Int]): UIDSet[SUID] = {
        EmptyUIDSet ++ l.map(i ⇒ SUID(i))
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
        val fl3 = UIDSet.empty[SUID] + s
        (fl1.head == s) :| "apply" && (fl2.head == s) :| "builder" && (fl3.head == s) :| "+"
    }

    property("singleton set after removing a potential second value") = forAll { (i: Int, j: Int) ⇒
        i != j ==> {
            UIDSet[SUID](i, j).tail.isSingletonSet :| "tail" &&
                (UIDSet[SUID](i) + j).filter(_ == SUID(i)).isSingletonSet :| "filter first value" &&
                (UIDSet[SUID](i) + j).filter(_ == SUID(j)).isSingletonSet :| "filter second value"
        }
    }

    property("create set(++)") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID.apply)
        val us = EmptyUIDSet ++ s
        (s.size == us.size) :| "size" &&
            s.forall(us.contains) :| "created set contains all values" &&
            us.forall(s.contains) :| "created set only contains added values"
    }

    property("create set(++ UIDSet)") = forAll { (a: Set[Int], b: Set[Int]) ⇒
        val ab: Set[SUID] = a.map(SUID.apply) ++ b.map(SUID.apply)
        val usa: UIDSet[SUID] = EmptyUIDSet ++ a.map(SUID.apply)
        val usb: UIDSet[SUID] = EmptyUIDSet ++ b.map(SUID.apply)
        val usab = usa ++ usb
        (ab.size == usab.size) :| "size" &&
            ab.forall(usab.contains) :| "created set contains all values" &&
            usab.forall(ab.contains) :| "created set only contains added values"
    }

    property("create set(+)") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        var us = EmptyUIDSet
        s.foreach { us += _ }
        s.size == us.size &&
            s.forall(us.contains) && us.forall(s.contains)
    }

    property("remove element (-)") = forAll { orig: Set[Int] ⇒
        val base = orig.map(SUID.apply)
        var s = base
        var us = EmptyUIDSet ++ s
        base.forall { e ⇒
            s -= e
            us -= e
            (s.size == us.size) && {
                if (!s.forall(us.contains)) {
                    println(
                        s"after removing $e s contains more elements: "+
                            s"$s(${s.getClass}) <-> $us(#${us.size}; ${us.getClass})"
                    )
                    false
                } else {
                    true
                }
            } && {
                if (!us.forall(s.contains)) {
                    println(s"after removing $e us contains more elements: $s <-> $us")
                    false
                } else {
                    true
                }
            }
        }
    }

    property("create set given two values") = forAll { (a: Int, b: Int) ⇒
        val us = UIDSet(SUID(a), SUID(b))
        (a == b && us.size == 1) || (a != b && us.size == 2)
    }

    property("==|hashCode[AnyRef]") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val us1 = UIDSet.empty[SUID] ++ s1.map(SUID(_))
        val us2 = UIDSet.empty[SUID] ++ s2.map(SUID(_))
        classify(s1 == s2, "both sets are equal") {
            (s1 == s2) == (us1 == us2) &&
                (us1 != us2 || us1.hashCode() == us2.hashCode())
        }
    }

    property("equals") = forAll { l: Set[Int] ⇒
        l.size >= 3 ==> {
            val p = l.toList.permutations
            val us1 = UIDSet.empty[SUID] ++ p.next.map(SUID.apply)
            val us2 = UIDSet.empty[SUID] ++ p.next.map(SUID.apply)
            val us3 = UIDSet.empty[SUID] ++ p.next.map(SUID.apply)
            val us4 = UIDSet.empty[SUID] ++ p.next.map(SUID.apply)
            (us1 == us2) && (us1 == us3) && (us1 == us4)
        }
    }

    property("seq") = forAll { l: List[Int] ⇒
        val fl = toSUIDSet(l)
        (fl.seq eq fl)
    }

    property("contains") = forAll { (s: Set[Int], i: Int) ⇒
        val us = toSUIDSet(s)
        us.contains(SUID(i)) == s.contains(i)
    }

    property("find") = forAll { (s: Set[Int]) ⇒
        def test(suid: SUID): Boolean = suid.id < 0
        val us = toSUIDSet(s)
        val ssuid = s.map(SUID(_))
        val usFound = us.find(test)
        val ssFound = ssuid.find(test)
        usFound.isDefined == ssFound.isDefined
    }

    property("iterator") = forAll { (s: Set[Int]) ⇒
        val us = toSUIDSet(s)
        us.iterator.toSet == s.map(SUID.apply)
    }

    property("toIdIterator") = forAll { (s: Set[Int]) ⇒
        val us = toSUIDSet(s)
        us.toIdIterator.toSet.iterator.toSet == s
    }

    property("toIdSet") = forAll { (s: Set[Int]) ⇒
        val us = toSUIDSet(s)
        us.toIdSet.iterator.toSet == s
    }

    property("last") = forAll { (s: Set[Int]) ⇒
        s.nonEmpty ==> {
            val us = toSUIDSet(s)
            us.last == us.iterator.toList.last
        }
    }

    property("compare(subsets)") = forAll { (s: Set[Int]) ⇒
        val us = toSUIDSet(s)
        classify(s.isEmpty, "the set is empty") {
            (s.isEmpty && us.compare(EmptyUIDSet) == EqualSets) || (
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

    property("head|tail") = forAll { (s: Set[Int]) ⇒
        var us = toSUIDSet(s)
        var seen = Set.empty[SUID]
        (0 until s.size).forall { i ⇒
            val result = !seen.contains(us.head)
            seen += us.head
            us = us.tail
            result
        } :| "forall" &&
            (seen == s.map(SUID.apply)) :| "repetition of value"
    }

    property("filter") = forAll { (s: Set[Int], i: Int) ⇒
        def test(s: SUID): Boolean = s.id < 0
        val us = toSUIDSet(s).filter(test)
        val expected = toSUIDSet(s.filter(i ⇒ test(SUID(i))))
        (us.size == expected.size) :| "size" && {
            if (expected != us) {
                println("expected: "+expected + expected.getClass+"; actual: "+us + us.getClass)
                false
            } else {
                true
            }
        }
    }

    property("filterNot") = forAll { (s: Set[Int], i: Int) ⇒
        def test(s: SUID): Boolean = s.id < 0
        val us = toSUIDSet(s).filterNot(test)
        toSUIDSet(s.filterNot(i ⇒ test(SUID(i)))) == us
    }

    property("intersect") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val us1 = toSUIDSet(s1)
        val us2 = toSUIDSet(s2)
        val us = toSUIDSet(s1 intersect s2)
        us == (us1 intersect us2)
    }

    property("foldLeft") = forAll { (s: Set[Int], i: Int) ⇒
        toSUIDSet(s).foldLeft(0)(_ + _.id * 2) == s.foldLeft(0)(_ + _.id * 2)
    }

    property("map (check that the same type of collection is created)") = forAll { (orig: Set[Int]) ⇒

        def f(s: SUID): SUID = SUID(s.id % 5)

        val s = orig.map(SUID(_)).map(f)
        val us: SUIDSet = toSUIDSet(orig).map(f)
        (s.forall(us.contains) && us.forall(s.contains)) :| s"content $s vs. $us" &&
            classOf[UIDSet[_]].isInstance(us) :| "unexpected type"
    }

    property("can handle large sets") = forAll(largeSetGen) { (v) ⇒
        val (orig: Set[Int], i) = v
        val us = toSUIDSet(orig)
        val usTransformed: UIDSet[SUID] =
            (us.
                map[SUID, UIDSet[SUID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100))

        val s = (Set.empty[SUID] ++ orig.map(SUID(_)))
        val sTransformed: Set[SUID] =
            (s.
                map[SUID, Set[SUID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100))

        classify(orig.size > 20, "original set is large") {
            classify(sTransformed.size > 20, "transformed set is large") {
                usTransformed.forall(sTransformed.contains) :| "us <= s" &&
                    sTransformed.forall(usTransformed.contains) :| "s <= us"
            }
        }
    }

    property("can efficiently create very large sets") = forAll(veryLargeListGen) { (v) ⇒
        val (orig: List[Int], _) = v
        val base = orig.map(SUID.apply)
        val s1 = /*org.opalj.util.PerformanceEvaluation.time*/ {
            val s1b = UIDSet.newBuilder[SUID]
            base.foreach(s1b.+=)
            s1b.result
        } //{ t ⇒ println("Builder +!: "+t.toSeconds) }

        var s2 = UIDSet.empty[SUID]
        //org.opalj.util.PerformanceEvaluation.time {
        base.foreach(e ⇒ s2 = s2 + e)
        //} { t ⇒ println("Builder + : "+t.toSeconds) }

        s1 == s2
    }

    property("can handle very large sets") = forAll(veryLargeListGen) { (v) ⇒
        val (orig: List[Int], i) = v
        val base = orig.map(SUID.apply)
        val usTransformed = {
            val us = UIDSet.empty[SUID] ++ base
            (us.
                map[SUID, UIDSet[SUID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100)) - SUID(1002)
        }

        val sTransformed = {
            val s = Set.empty[SUID] ++ base
            (s.
                map[SUID, Set[SUID]] { e ⇒ SUID(e.id % i) }.
                filter(_.id < i / 2).
                +(SUID(i)) + SUID(-i) + SUID(i + 100)) - SUID(1002)
        }

        classify(base.size > 25000, s"original set is very large (>25000)") {
            classify(sTransformed.size > 25000, "transformed set is still very large (>25000)") {
                usTransformed.forall(sTransformed.contains) :| "us <= s" &&
                    sTransformed.forall(usTransformed.contains) :| "s <= us"
            }
        }
    }

    // METHODS DEFINED BY UIDSet

    property("isSingletonSet") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptyUIDSet ++ s
        (s.size == 1) == us.isSingletonSet
    }
}
