/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.propBoolean
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation

/**
 * Tests `UIDSets` by creating standard Sets and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UIDSetTest extends AnyFunSpec with Matchers with ScalaCheckDrivenPropertyChecks {

    describe("properties") {

        type SUIDSet = UIDSet[SUID]
        val EmptyUIDSet: SUIDSet = UIDSet.empty[SUID]

        implicit def intToSUID(i: Int): SUID = SUID(i)

        implicit def toSUIDSet(l: Iterable[Int]): UIDSet[SUID] = {
            EmptyUIDSet ++ l.map(i => SUID(i))
        }

        val veryLargeListGen = for {
            i <- Gen.choose(30000, 100000)
            s <- Gen.containerOfN[List, Int](i, Arbitrary.arbitrary[Int])
        } yield (s, i)

        val largeSetGen = for {
            i <- Gen.choose(20, 100)
            s <- Gen.containerOfN[Set, Int](i, Arbitrary.arbitrary[Int])
        } yield (s, i)

        it("create singleton set") {
            forAll { i: Int =>
                val s = SUID(i)
                val fl1 = UIDSet[SUID](i)
                val fl2 = (UIDSet.newBuilder[SUID] += i).result()
                val fl3 = UIDSet.empty[SUID] + s
                (fl1.head == s) :| "apply" && (fl2.head == s) :| "builder" && (fl3.head == s) :| "+"
            }
        }

        it("singleton set after removing a potential second value") {
            forAll { (i: Int, j: Int) =>
                i != j ==> {
                    UIDSet[SUID](i, j).tail.isSingletonSet :| "tail" &&
                        (UIDSet[SUID](i) + j).filter(_ == SUID(i)).isSingletonSet :| "filter first value" &&
                        (UIDSet[SUID](i) + j).filter(_ == SUID(j)).isSingletonSet :| "filter second value"
                }
            }
        }

        it("create set(++)") {
            forAll { orig: Set[Int] =>
                val s = orig.map(SUID.apply)
                val us = EmptyUIDSet ++ s
                (s.size == us.size) :| "size" &&
                    s.forall(us.contains) :| "created set contains all values" &&
                    us.forall(s.contains) :| "created set only contains added values"
            }
        }

        it("create set(++ UIDSet)") {
            forAll { (a: Set[Int], b: Set[Int]) =>
                val ab: Set[SUID] = a.map(SUID.apply) ++ b.map(SUID.apply)
                val usa: UIDSet[SUID] = EmptyUIDSet ++ a.map(SUID.apply)
                val usb: UIDSet[SUID] = EmptyUIDSet ++ b.map(SUID.apply)
                val usab = usa ++ usb
                (ab.size == usab.size) :| "size" &&
                    ab.forall(usab.contains) :| "created set contains all values" &&
                    usab.forall(ab.contains) :| "created set only contains added values"
            }
        }

        it("create set(+)") {
            forAll { orig: Set[Int] =>
                val s = orig.map(SUID(_))
                var us = EmptyUIDSet
                s.foreach {
                    us += _
                }
                s.size == us.size &&
                    s.forall(us.contains) && us.forall(s.contains)
            }
        }

        it("remove element (-)") {
            forAll { orig: Set[Int] =>
                val base = orig.map(SUID.apply)
                var s = base
                var us = EmptyUIDSet ++ s
                base.forall { e =>
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
        }

        it("create set given two values") {
            forAll { (a: Int, b: Int) =>
                val us = UIDSet(SUID(a), SUID(b))
                (a == b && us.size == 1) || (a != b && us.size == 2)
            }
        }

        it("==|hashCode[AnyRef]") {
            forAll { (s1: Set[Int], s2: Set[Int]) =>
                val us1 = UIDSet.empty[SUID] ++ s1.map(SUID(_))
                val us2 = UIDSet.empty[SUID] ++ s2.map(SUID(_))
                classify(s1 == s2, "both sets are equal") {
                    (s1 == s2) == (us1 == us2) &&
                        (us1 != us2 || us1.hashCode() == us2.hashCode())
                }
            }
        }

        it("equals") {
            forAll { l: Set[Int] =>
                l.size >= 3 ==> {
                    val p = l.toList.permutations
                    val us1 = UIDSet.empty[SUID] ++ p.next().map(SUID.apply)
                    val us2 = UIDSet.empty[SUID] ++ p.next().map(SUID.apply)
                    val us3 = UIDSet.empty[SUID] ++ p.next().map(SUID.apply)
                    val us4 = UIDSet.empty[SUID] ++ p.next().map(SUID.apply)
                    (us1 == us2) && (us1 == us3) && (us1 == us4)
                }
            }
        }

        it("seq") {
            forAll { l: List[Int] =>
                val fl = toSUIDSet(l)
                (fl.toSeq eq fl)
            }
        }

        it("contains") {
            forAll { (s: Set[Int], i: Int) =>
                val us = toSUIDSet(s)
                us.contains(SUID(i)) == s.contains(i)
            }
        }

        it("find") {
            forAll { (s: Set[Int]) =>
                def test(suid: SUID): Boolean = suid.id < 0

                val us = toSUIDSet(s)
                val ssuid = s.map(SUID(_))
                val usFound = us.find(test)
                val ssFound = ssuid.find(test)
                usFound.isDefined == ssFound.isDefined
            }
        }

        it("findById") {
            forAll { (s: Set[Int], e: Set[Int]) =>
                val us = toSUIDSet(s) ++ UIDSet(e.slice(0, e.size / 2).map(SUID.apply).toSeq: _*)
                classify(us.size > 0, "non-empty set") {
                    e.forall(v => us.find(_.id == v) == us.findById(v))
                }
            }
        }

        it("iterator") {
            forAll { (s: Set[Int]) =>
                val us = toSUIDSet(s)
                us.iterator.toSet == s.map(SUID.apply)
            }
        }

        it("idIterator") {
            forAll { (s: Set[Int]) =>
                val us = toSUIDSet(s)
                val usIdSet = us.idIterator.toSet
                usIdSet.size == s.size && s.forall(usIdSet.contains)
            }
        }

        it("idSet") {
            forAll { (s: Set[Int]) =>
                val us = toSUIDSet(s)
                val usIdSet = us.idSet
                usIdSet.size == s.size && s.forall(usIdSet.contains)
            }
        }

        it("last") {
            forAll { (s: Set[Int]) =>
                s.nonEmpty ==> {
                    val us = toSUIDSet(s)
                    us.last == us.iterator.toList.last
                }
            }
        }

        it("compare(subsets)") {
            forAll { (s: Set[Int]) =>
                val us = toSUIDSet(s)
                classify(s.isEmpty, "the set is empty") {
                    (s.isEmpty && us.compare(EmptyUIDSet) == EqualSets) || (
                        (s.tail.inits.forall(init => toSUIDSet(init).compare(us) == StrictSubset)) &&
                        (s.tail.inits.forall(init => us.compare(toSUIDSet(init)) == StrictSuperset))
                    )
                }
            }
        }

        it("compare(arbitrary sets)") {
            forAll { (s1: Set[Int], s2: Set[Int]) =>
                val us1 = toSUIDSet(s1)
                val us2 = toSUIDSet(s2)
                val us1CompareUs2 = us1.compare(us2)
                val us2CompareUs1 = us2.compare(us1)
                (s1 == s2 && us1CompareUs2 == us2CompareUs1 && us1CompareUs2 == EqualSets) ||
                    (s1.subsetOf(s2) && us1CompareUs2 == StrictSubset && us2CompareUs1 == StrictSuperset) ||
                    (s2.subsetOf(s1) && us2CompareUs1 == StrictSubset && us1CompareUs2 == StrictSuperset) ||
                    (us1CompareUs2 == us2CompareUs1 && us1CompareUs2 == UncomparableSets)
            }
        }

        it("head|tail") {
            forAll { (s: Set[Int]) =>
                var us = toSUIDSet(s)
                var seen = Set.empty[SUID]
                (0 until s.size).forall { i =>
                    val result = !seen.contains(us.head)
                    seen += us.head
                    us = us.tail
                    result
                } :| "forall" &&
                    (seen == s.map(SUID.apply)) :| "repetition of value"
            }
        }

        it("filter") {
            forAll { (s: Set[Int], i: Int) =>
                def test(s: SUID): Boolean = s.id < 0

                val us = toSUIDSet(s).filter(test)
                val expected = toSUIDSet(s.filter(i => test(SUID(i))))
                (us.size == expected.size) :| "size" && {
                    if (expected != us) {
                        println("expected: "+expected + expected.getClass+"; actual: "+us + us.getClass)
                        false
                    } else {
                        true
                    }
                }
            }
        }

        it("filterNot") {
            forAll { (s: Set[Int], i: Int) =>
                def test(s: SUID): Boolean = s.id < 0

                val us = toSUIDSet(s).filterNot(test)
                toSUIDSet(s.filterNot(i => test(SUID(i)))) == us
            }
        }

        it("intersect") {
            forAll { (s1: Set[Int], s2: Set[Int]) =>
                val us1 = toSUIDSet(s1)
                val us2 = toSUIDSet(s2)
                val us = toSUIDSet(s1 intersect s2)
                us == (us1 intersect us2)
            }
        }

        it("foldLeft") {
            forAll { (s: Set[Int], i: Int) =>
                toSUIDSet(s).foldLeft(0)(_ + _.id * 2) == s.foldLeft(0)(_ + _.id * 2)
            }
        }

        it("map (check that the same type of collection is created)") {
            val orig = Set(1, -1, 0, -2)
            def f(s: SUID): SUID = SUID(s.id % 5)

            val s = orig.map(SUID(_)).map(f)
            val uidSet = toSUIDSet(orig)
            val us: SUIDSet = uidSet.mapUIDSet(f)
            (s.forall(us.contains) && us.forall(s.contains)) :| s"content $s vs. $us" &&
                classOf[UIDSet[_]].isInstance(us) :| "unexpected type"
            //            forAll { (orig: Set[Int]) =>
            //
            //                def f(s: SUID): SUID = SUID(s.id % 5)
            //
            //                val s = orig.map(SUID(_)).map(f)
            //                val us: SUIDSet = toSUIDSet(orig).mapUIDSet(f)
            //                (s.forall(us.contains) && us.forall(s.contains)) :| s"content $s vs. $us" &&
            //                    classOf[UIDSet[_]].isInstance(us) :| "unexpected type"
            //            }
        }

        it("can handle large sets") {
            forAll(largeSetGen) { (v) =>
                val (orig: Set[Int], i) = v
                val us = toSUIDSet(orig)
                val usTransformed: UIDSet[SUID] =
                    (us.
                        mapUIDSet[SUID] { e => SUID(e.id % i) }.
                        filter(_.id < i / 2).
                        +(SUID(i)) + SUID(-i) + SUID(i + 100))

                val s = (Set.empty[SUID] ++ orig.map(SUID(_)))
                val sTransformed: Set[SUID] =
                    (s.
                        map[SUID] { e => SUID(e.id % i) }.
                        filter(_.id < i / 2).
                        +(SUID(i)) + SUID(-i) + SUID(i + 100))

                classify(orig.size > 20, "original set is large") {
                    classify(sTransformed.size > 20, "transformed set is large") {
                        usTransformed.forall(sTransformed.contains) :| "us <= s" &&
                            sTransformed.forall(usTransformed.contains) :| "s <= us"
                    }
                }
            }
        }

        it("can efficiently create very large sets") {
            forAll(veryLargeListGen) { (v) =>
                val (orig: List[Int], _) = v
                val base = orig.map(SUID.apply)
                val s1 = /*org.opalj.util.PerformanceEvaluation.time*/ {
                    val s1b = UIDSet.newBuilder[SUID]
                    base.foreach(s1b.+=)
                    s1b.result()
                } //{ t => println("Builder +!: "+t.toSeconds) }

                var s2 = UIDSet.empty[SUID]
                //org.opalj.util.PerformanceEvaluation.time {
                base.foreach(e => s2 = s2 + e)
                //} { t => println("Builder + : "+t.toSeconds) }

                s1 == s2
            }
        }

        it("can handle very large sets") {
            forAll(veryLargeListGen) { (v) =>
                val (orig: List[Int], i) = v
                val base = orig.map(SUID.apply)
                val usTransformed = {
                    val us = UIDSet.empty[SUID] ++ base
                    (us.
                        map[SUID] { e => SUID(e.id % i) }.
                        filter(_.id < i / 2).
                        +(SUID(i)) + SUID(-i) + SUID(i + 100)) - SUID(1002)
                }

                val sTransformed = {
                    val s = Set.empty[SUID] ++ base
                    (s.
                        map[SUID] { e => SUID(e.id % i) }.
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
        }

        // METHODS DEFINED BY UIDSet

        it("isSingletonSet") {
            forAll { orig: Set[Int] =>
                val s = orig.map(SUID(_))
                val us = EmptyUIDSet ++ s
                (s.size == 1) == us.isSingletonSet
            }
        }

        it("add, remove, filter") {
            forAll { (a: Set[Int], b: Set[Int]) =>
                val aus = UIDSet.empty[SUID] ++ a.map(SUID.apply)
                val toBeRemoved = b.size / 2
                val newAUS = ((aus ++ b.map(SUID.apply)) -- b.slice(0, toBeRemoved).map(SUID.apply)).filter(i => b.contains(i.id))
                val newA = (a ++ b -- b.slice(0, toBeRemoved)).filter(b.contains)
                classify(newA.size == 0, "new A is now empty") {
                    classify(newA.size < a.size, "new A is smaller than a") {
                        newAUS.size == newA.size &&
                            newAUS.iterator.map[Int](_.id).toSet == newA
                    }
                }
            }
        }
    }

    describe("performance") {

        it("foreach") {
            {
                val seed = 123456789L
                val rngGen = new java.util.Random(seed)

                var overallSum = 0
                var overallTime = Nanoseconds.None
                for { i <- 1 to 100000 } {
                    var s = UIDSet.empty[SUID]
                    for { j <- 1 to 500 } {
                        s += SUID(rngGen.nextInt())
                    }

                    var sumForeach = 0
                    PerformanceEvaluation.time {
                        s.foreach(sumForeach += _.id)
                    } { t => overallTime += t }

                    overallSum += sumForeach
                }
                info(s"UIDSet foreach sum took ${overallTime.toSeconds}")
            }

            {
                val seed = 123456789L
                val rngGen = new java.util.Random(seed)

                var overallSum = 0
                var overallTime = Nanoseconds.None
                for { i <- 1 to 100000 } {
                    var s = Set.empty[SUID]
                    for { j <- 1 to 500 } {
                        s += SUID(rngGen.nextInt())
                    }

                    var sumForeach = 0
                    PerformanceEvaluation.time {
                        s.foreach(sumForeach += _.id)
                    } { t => overallTime += t }

                    overallSum += sumForeach
                }
                info(s"Set[UID] foreach sum took ${overallTime.toSeconds}")
            }
        }

        it("time for creating the sets compared to Set[UID]") {
            val opalS = {
                var totalSize = 0
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var lastOpalS = UIDSet.empty[SUID]
                    for { j <- 0 to 1000000 } {
                        var opalS = UIDSet.empty[SUID]
                        for { i <- 0 to 50 } {
                            val v = rngGen.nextInt()
                            opalS += SUID(v)
                        }
                        totalSize += opalS.size
                        lastOpalS = opalS
                    }
                    lastOpalS
                } { t => info(s"UIDSet took ${t.toSeconds} (total number of entries: $totalSize)") }
            }

            val scalaS = {
                var totalSize = 0
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var lastScalaS = Set.empty[SUID]
                    for { j <- 0 to 1000000 } {
                        var scalaS = Set.empty[SUID]
                        for { i <- 0 to 50 } {
                            val v = rngGen.nextInt()
                            scalaS += SUID(v)
                        }
                        totalSize += scalaS.size
                        lastScalaS = scalaS
                    }
                    lastScalaS
                } { t => info(s"Set[UID] took ${t.toSeconds} (total number of entries: $totalSize)") }
            }

            var opalTotal = 0L
            for { v <- opalS } { opalTotal += v.id }
            var scalaTotal = 0L
            for { v <- scalaS } { scalaTotal += v.id }
            assert(opalTotal == scalaTotal)
        }

        it("memory for creating the sets compared to Set[UID]") {
            val opalS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var allSets = List.empty[UIDSet[SUID]]
                    for { j <- 0 to 1000000 } {
                        var opalS = UIDSet.empty[SUID]
                        for { i <- 0 to 50 } {
                            val v = rngGen.nextInt()
                            opalS += SUID(v)
                        }
                        allSets ::= opalS
                    }
                    allSets
                } { t => info(s"UIDSet took ${t.toSeconds}") }
            } { mu => info(s"UIDSet required $mu bytes") }

            val scalaS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var allSets = List.empty[Set[SUID]]
                    for { j <- 0 to 1000000 } {
                        var scalaS = Set.empty[SUID]
                        for { i <- 0 to 50 } {
                            val v = rngGen.nextInt()
                            scalaS += SUID(v)
                        }
                        allSets ::= scalaS
                    }
                    allSets
                } { t => info(s"Set[UID] took ${t.toSeconds}") }
            } { mu => info(s"Set[UID] required $mu bytes") }

            info(s"overall size of sets: "+scalaS.map(_.size).sum)
            assert(opalS.map(_.size).sum == scalaS.map(_.size).sum)
        }
    }

    describe("the equals operation for sets of one value") {
        it("should return true for two sets containing the same value") {
            assert(new UIDSet1(SUID(1)) == UIDSet1(SUID(1)))

            assert(
                new UIDSetInnerNode[SUID](1, SUID(1), null, null) == UIDSet1(SUID(1))
            )
        }
    }

    describe("the equals operation for sets of two values") {

        it("should return true for two new sets containing the same values") {
            assert(new UIDSet2(SUID(1), SUID(2)) == new UIDSet2(SUID(2), SUID(1)))

            assert(
                new UIDSet3(SUID(1), SUID(2), SUID(3)).filter(_.id != 2)
                    == new UIDSet2(SUID(3), SUID(1))
            )
            assert(
                (UIDSet.empty[SUID] + SUID(1) + SUID(2) + SUID(3) + SUID(4)).filter(_.id != 2) - SUID(1)
                    == new UIDSet2(SUID(3), SUID(4))
            )

            assert(
                new UIDSet2(SUID(3), SUID(1)) ==
                    new UIDSet3(SUID(1), SUID(2), SUID(3)).filter(_.id != 2)
            )
            assert(
                new UIDSet2(SUID(3), SUID(4)) ==
                    (UIDSet.empty[SUID] + SUID(1) + SUID(2) + SUID(3) + SUID(4)).filter(_.id != 2) - SUID(1)
            )
        }
    }

    describe("the equals operation for sets of three values") {

        it("should return true for two new sets containing the same values") {
            assert(
                new UIDSet2(SUID(3), SUID(2)) + SUID(1) == new UIDSet3(SUID(2), SUID(1), SUID(3))
            )
            assert(
                new UIDSet2(SUID(1), SUID(2)) + SUID(3) == new UIDSet3(SUID(2), SUID(1), SUID(3))
            )

            assert(
                new UIDSet3(SUID(1), SUID(2), SUID(3)) ==
                    (UIDSet.empty[SUID] + SUID(1) + SUID(2) + SUID(3) + SUID(4)).filter(_.id != 4)
            )
        }
    }

    describe("the equals operation for sets of four values") {

        it("should return true for two new sets containing the same values") {
            assert(
                new UIDSetInnerNode[SUID](1, SUID(1), null, null) + SUID(2) + SUID(3) + SUID(4) ==
                    new UIDSetInnerNode[SUID](1, SUID(4), null, null) + SUID(3) + SUID(2) + SUID(1)
            )
            assert(
                new UIDSetInnerNode[SUID](1, SUID(1), null, null) + SUID(2) + SUID(3) + SUID(4) ==
                    new UIDSetInnerNode[SUID](1, SUID(2), null, null) + SUID(4) + SUID(1) + SUID(3)
            )
            assert(
                new UIDSetInnerNode[SUID](1, SUID(1), null, null) + SUID(2) + SUID(3) + SUID(4) ==
                    new UIDSet3[SUID](SUID(1), SUID(4), SUID(2)) + SUID(3)
            )
        }
    }
}

case class SUID(id: Int) extends UID
