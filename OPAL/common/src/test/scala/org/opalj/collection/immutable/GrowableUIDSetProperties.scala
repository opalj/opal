/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation

@RunWith(classOf[JUnitRunner])
object GrowableUIDSetProperties extends Properties("GrowableUIDSet") {

    implicit def intToSUID(i: Int): SUID = SUID(i)
    implicit def toSUIDSet(l: Traversable[Int]): GrowableUIDTrieSet[SUID] = {
        l.foldLeft(GrowableUIDTrieSet.empty[SUID])(_ + SUID(_))
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                             G E N E R A T O R S

    val intSetGen: Gen[Set[Int]] = Gen.containerOf[Set, Int](Gen.posNum[Int])

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

    property("create set(+) and contains") = forAll { intSet: Set[Int] ⇒
        val oUIDTrieSet = intSet.foldLeft(GrowableUIDTrieSet.empty[SUID])(_ + _)

        val sUIDSet = intSet.map(SUID(_))
        (sUIDSet.size == oUIDTrieSet.size) :| "size" &&
            sUIDSet.forall(oUIDTrieSet.contains) :| "oUIDTrieSet.contains" &&
            oUIDTrieSet.forall(sUIDSet.contains) :| "oUIDTrieSet.forall"
    }

    property("create set(+) and foreach") = forAll { intSet: Set[Int] ⇒
        val oUIDTrieSet = intSet.foldLeft(GrowableUIDTrieSet.empty[SUID])(_ + _)
        val sUIDSet = intSet.map(SUID(_))
        var containsAll = true
        oUIDTrieSet.foreach { e ⇒
            containsAll &&= sUIDSet.contains(e)
        }
        containsAll
    }

    property("create set(+) and iterator") = forAll { intSet: Set[Int] ⇒
        val oUIDTrieSet = intSet.foldLeft(GrowableUIDTrieSet.empty[SUID])(_ + _)
        val sUIDSet = intSet.map(SUID(_))
        oUIDTrieSet.iterator.forall(sUIDSet.contains)
    }

    property("equals and hashCode (if the sets are equal)") = forAll { l: Set[Int] ⇒
        val p = l.toList.permutations
        val lastP = p.next
        val uidSet1 = toSUIDSet(if (p.hasNext) p.next else lastP)
        val uidSet2 = toSUIDSet(if (p.hasNext) p.next else lastP)
        val uidSet3 = toSUIDSet(if (p.hasNext) p.next else lastP)
        val uidSet4 = toSUIDSet(if (p.hasNext) p.next else lastP)
        (uidSet1 == uidSet2 && uidSet2 == uidSet3 && uidSet3 == uidSet4 && uidSet1 == uidSet4) :| "equality" &&
            (uidSet1.hashCode == uidSet2.hashCode) :| s"hashCodes 1 vs 2: ${uidSet1.hashCode} vs. ${uidSet2.hashCode}" &&
            (uidSet2.hashCode == uidSet3.hashCode) :| s"hashCodes 2 vs 3: ${uidSet2.hashCode} vs. ${uidSet3.hashCode}" &&
            (uidSet3.hashCode == uidSet4.hashCode) :| s"hashCodes 3 vs 4: ${uidSet3.hashCode} vs. ${uidSet4.hashCode}"
    }
}

@RunWith(classOf[JUnitRunner])
class GrowableUIDTrieSetTest extends FunSpec with Matchers {

    describe("performance") {

        it("foreach") {
            {
                val seed = 123456789L
                val rngGen = new java.util.Random(seed)

                var overallSum = 0
                var overallTime = Nanoseconds.None
                for { i ← 1 to 100000 } {
                    var s = GrowableUIDTrieSet.empty[SUID]
                    for { j ← 1 to 500 } {
                        s += SUID(rngGen.nextInt())
                    }

                    var sumForeach = 0
                    PerformanceEvaluation.time {
                        s.foreach(sumForeach += _.id)
                    } { t ⇒ overallTime += t }

                    overallSum += sumForeach
                }
                info(s"GrowableUIDTrieSet foreach sum took ${overallTime.toSeconds}")
            }

            {
                val seed = 123456789L
                val rngGen = new java.util.Random(seed)

                var overallSum = 0
                var overallTime = Nanoseconds.None
                for { i ← 1 to 100000 } {
                    var s = Set.empty[SUID]
                    for { j ← 1 to 500 } {
                        s += SUID(rngGen.nextInt())
                    }

                    var sumForeach = 0
                    PerformanceEvaluation.time {
                        s.foreach(sumForeach += _.id)
                    } { t ⇒ overallTime += t }

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
                    var lastOpalS = GrowableUIDTrieSet.empty[SUID]
                    for { j ← 0 to 1000000 } {
                        var opalS = GrowableUIDTrieSet.empty[SUID]
                        for { i ← 0 to 50 } {
                            val v = rngGen.nextInt()
                            opalS += SUID(v)
                        }
                        totalSize += opalS.size
                        lastOpalS = opalS
                    }
                    lastOpalS
                } { t ⇒ info(s"GrowableUIDTrieSet took ${t.toSeconds} (total number of entries: $totalSize)") }
            }

            val scalaS = {
                var totalSize = 0
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var lastScalaS = Set.empty[SUID]
                    for { j ← 0 to 1000000 } {
                        var scalaS = Set.empty[SUID]
                        for { i ← 0 to 50 } {
                            val v = rngGen.nextInt()
                            scalaS += SUID(v)
                        }
                        totalSize += scalaS.size
                        lastScalaS = scalaS
                    }
                    lastScalaS
                } { t ⇒ info(s"Set[UID] took ${t.toSeconds} (total number of entries: $totalSize)") }
            }

            var opalTotal = 0L
            for { v ← opalS } { opalTotal += v.id }
            var scalaTotal = 0L
            for { v ← scalaS } { scalaTotal += v.id }
            assert(opalTotal == scalaTotal)
        }

        it("memory for creating the sets compared to Set[UID]") {
            val opalS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var allSets = List.empty[GrowableUIDTrieSet[SUID]]
                    for { j ← 0 to 1000000 } {
                        var opalS = GrowableUIDTrieSet.empty[SUID]
                        for { i ← 0 to 50 } {
                            val v = rngGen.nextInt()
                            opalS += SUID(v)
                        }
                        allSets ::= opalS
                    }
                    allSets
                } { t ⇒ info(s"GrowableUIDTrieSet took ${t.toSeconds}") }
            } { mu ⇒ info(s"GrowableUIDTrieSet required $mu bytes") }

            val scalaS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var allSets = List.empty[Set[SUID]]
                    for { j ← 0 to 1000000 } {
                        var scalaS = Set.empty[SUID]
                        for { i ← 0 to 50 } {
                            val v = rngGen.nextInt()
                            scalaS += SUID(v)
                        }
                        allSets ::= scalaS
                    }
                    allSets
                } { t ⇒ info(s"Set[UID] took ${t.toSeconds}") }
            } { mu ⇒ info(s"Set[UID] required $mu bytes") }

            info(s"overall size of sets: "+scalaS.map(_.size).sum)
            assert(opalS.map(_.size).sum == scalaS.map(_.size).sum)
        }
    }
}
