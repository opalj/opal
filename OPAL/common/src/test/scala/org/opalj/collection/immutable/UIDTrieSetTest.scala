/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import scala.language.implicitConversions
import org.scalacheck.Prop.propBoolean
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation

@RunWith(classOf[JUnitRunner])
class UIDTrieSetTest extends AnyFunSpec with ScalaCheckDrivenPropertyChecks with Matchers {

    describe("properties") {

        implicit def intToSUID(i: Int): SUID = SUID(i)
        implicit def toSUIDSet(l: Iterable[Int]): UIDTrieSet[SUID] = {
            l.foldLeft(UIDTrieSet.empty[SUID])(_ add SUID(_))
        }

        it("create set(+) and forall and contains(id)") {
            forAll { intSet: Set[Int] =>
                val oUIDTrieSet = intSet.foldLeft(UIDTrieSet.empty[SUID])(_ add _)

                val sUIDSet = intSet.map(SUID(_))
                (sUIDSet.isEmpty == oUIDTrieSet.isEmpty) :| "isEmpty" &&
                    ((sUIDSet.size == 1 && oUIDTrieSet.isSingletonSet) || sUIDSet.size != 1) :| "isSingletonSet" &&
                    (sUIDSet.size == oUIDTrieSet.size) :| "size" &&
                    sUIDSet.forall(oUIDTrieSet.contains) :| "oUIDTrieSet.contains" &&
                    oUIDTrieSet.forall(sUIDSet.contains) :| "oUIDTrieSet.forall"
            }
        }

        it("create set(+) and foreach") {
            forAll { intSet: Set[Int] =>
                val oUIDTrieSet = intSet.foldLeft(UIDTrieSet.empty[SUID])(_ add _)
                val sUIDSet = intSet.map(SUID(_))
                var newSUIDSet = Set.empty[SUID]
                oUIDTrieSet.foreach { e => newSUIDSet += e }
                sUIDSet == newSUIDSet
            }
        }

        it("create set(+) and iterator") {
            forAll { intSet: Set[Int] =>
                val oUIDTrieSet = intSet.foldLeft(UIDTrieSet.empty[SUID])(_ add _)
                val sUIDSet = intSet.map(SUID(_))
                var newSUIDSet = Set.empty[SUID]
                oUIDTrieSet.iterator.foreach { e => newSUIDSet += e }
                sUIDSet == newSUIDSet
            }
        }

        it("equals and hashCode (if the sets are equal)") {
            forAll { intSet: Set[Int] =>
                val p = intSet.toList.permutations
                val initialUIDTrieSet = toSUIDSet(p.next())
                p.take(100).forall { p =>
                    val pUIDTrieSet = toSUIDSet(p)
                    initialUIDTrieSet == pUIDTrieSet &&
                        initialUIDTrieSet.hashCode == pUIDTrieSet.hashCode
                }
            }
        }

        it("equals (if the sets are not equal)") {
            forAll { intSet: Set[Int] =>
                intSet.nonEmpty ==> {
                    val oUIDTrieSet = toSUIDSet(intSet)
                    oUIDTrieSet.iterator.toList.tail.tails.forall { tailUIDTrieSet =>
                        tailUIDTrieSet.foldLeft(UIDTrieSet.empty[SUID])(_ add _) != oUIDTrieSet
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
                    var s = UIDTrieSet.empty[SUID]
                    for { j <- 1 to 500 } {
                        s = s add SUID(rngGen.nextInt())
                    }

                    var sumForeach = 0
                    PerformanceEvaluation.time {
                        s.foreach(sumForeach += _.id)
                    } { t => overallTime += t }

                    overallSum += sumForeach
                }
                info(s"UIDTrieSet foreach sum took ${overallTime.toSeconds}")
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
                    var lastOpalS = UIDTrieSet.empty[SUID]
                    for { j <- 0 to 1000000 } {
                        var opalS = UIDTrieSet.empty[SUID]
                        for { i <- 0 to 50 } {
                            val v = rngGen.nextInt()
                            opalS = opalS add SUID(v)
                        }
                        totalSize += opalS.size
                        lastOpalS = opalS
                    }
                    lastOpalS
                } { t => info(s"UIDTrieSet took ${t.toSeconds} (total number of entries: $totalSize)") }
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
                    var allSets = List.empty[UIDTrieSet[SUID]]
                    for { j <- 0 to 1000000 } {
                        var opalS = UIDTrieSet.empty[SUID]
                        for { i <- 0 to 50 } {
                            val v = rngGen.nextInt()
                            opalS = opalS add SUID(v)
                        }
                        allSets ::= opalS
                    }
                    allSets
                } { t => info(s"UIDTrieSet took ${t.toSeconds}") }
            } { mu => info(s"UIDTrieSet required $mu bytes") }

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
}
