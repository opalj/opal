/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FunSpec

import org.opalj.util.PerformanceEvaluation

@RunWith(classOf[JUnitRunner])
class GrowableIntTrieSetTest extends FunSpec with Matchers {

    describe("regression tests") {

        it("-149916 + -102540 + -118018 + -91539 + 0") {
            val s = GrowableIntTrieSet1(-149916) + -102540 + -118018 + -91539 + 0
            assert(s.size == 5)
            assert(s.contains(-149916))
            assert(s.contains(-102540))
            assert(s.contains(-118018))
            assert(s.contains(-91539))
            assert(s.contains(0))
        }

        it("-149916 + -102540 + -118018 + -91539 + 0 + -149916 + -102540 + -118018 + -91539 + 0 ") {
            val s = GrowableIntTrieSet.empty +
                -149916 + -102540 + -118018 + -91539 + 0 +
                -149916 + -102540 + -118018 + -91539 + 0
            assert(s.size == 5)
            assert(s.contains(-149916))
            assert(s.contains(-102540))
            assert(s.contains(-118018))
            assert(s.contains(-91539))
            assert(s.contains(0))
        }
    }

    describe("processing an GrowableIntTrieSet which is leaning to the right") {

        val s = GrowableIntTrieSet1(8192) + 16384 + 32768 + 65536 + 131072

        it("it should contain the given values") {
            assert(s.contains(8192))
            assert(s.contains(16384))
            assert(s.contains(32768))
            assert(s.contains(65536))
            assert(s.contains(131072))
        }
    }

    describe(s"performance") {

        it("when comparing with Set[Int]") {
            val opalS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var opalS = org.opalj.collection.immutable.GrowableIntTrieSet.empty
                    for { i ← 0 to 1000000 } {
                        val v = rngGen.nextInt()
                        opalS += v
                    }
                    opalS
                } { t ⇒ info(s"GrowableIntTrieSet took ${t.toSeconds}") }
            } { mu ⇒ info(s"GrowableIntTrieSet required $mu bytes") }

            val scalaS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var scalaS = Set.empty[Int]
                    for { i ← 0 to 1000000 } {
                        val v = rngGen.nextInt()
                        scalaS += v
                    }
                    scalaS
                } { t ⇒ info(s"Set[Int] took ${t.toSeconds}") }
            } { mu ⇒ info(s"Set[Int] required $mu bytes") }

            var opalTotal = 0L
            PerformanceEvaluation.time {
                for { v ← opalS } { opalTotal += v }
            } { t ⇒ info(s"OPAL ${t.toSeconds} for foreach") }

            var scalaTotal = 0L
            PerformanceEvaluation.time {
                for { v ← scalaS } { scalaTotal += v }
            } { t ⇒ info(s"Scala ${t.toSeconds} for foreach") }

            assert(opalTotal == scalaTotal)
        }

        it("for small sets (up to 8 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i ← 1 to 3 } rngGen.nextInt();
            val setValues = (for { i ← 1 to 1000 } yield Math.abs(rngGen.nextInt())).toArray
            val queryValues = (for { i ← 1 to 1000 } yield Math.abs(rngQuery.nextInt())).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 10000000 } {
                    var s = org.opalj.collection.immutable.GrowableIntTrieSet.empty
                    var hits = 0
                    for { i ← 0 to rngGen.nextInt(8) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t ⇒ info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for small sets (8 to 16 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i ← 1 to 3 } rngGen.nextInt();
            val setValues = (for { i ← 1 to 1000 } yield Math.abs(rngGen.nextInt())).toArray
            val queryValues = (for { i ← 1 to 1000 } yield Math.abs(rngQuery.nextInt())).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 10000000 } {
                    var s = org.opalj.collection.immutable.GrowableIntTrieSet.empty
                    var hits = 0
                    for { i ← 0 to 8 + rngGen.nextInt(8) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t ⇒ info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for small sets (16 to 32 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i ← 1 to 16 } rngGen.nextInt();
            val setValues = (for { i ← 1 to 10000 } yield Math.abs(rngGen.nextInt())).toArray
            val queryValues = (for { i ← 1 to 10000 } yield Math.abs(rngQuery.nextInt())).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 1000000 } {
                    var s = org.opalj.collection.immutable.GrowableIntTrieSet.empty
                    var hits = 0
                    for { i ← 0 to 16 + rngGen.nextInt(16) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t ⇒ info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for sets with up to 10000 elements creation and contains check should finish in reasonable time") {
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i ← 1 to 3333 } rngGen.nextInt();
            val setValues = (for { i ← 1 to 10000 } yield rngGen.nextInt()).toArray
            val queryValues = (for { i ← 1 to 10000 } yield rngQuery.nextInt()).toArray

            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            PerformanceEvaluation.time {
                for { runs ← 0 until 10000 } {
                    var s = org.opalj.collection.immutable.GrowableIntTrieSet.empty
                    var hits = 0
                    for { i ← 1 to runs } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t ⇒ info(s"${t.toSeconds} to create 10_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("operations on 2500 sets with ~10000 elements each") {
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)

            val allSets = PerformanceEvaluation.memory {
                for {
                    set ← 0 until 2500
                } yield {
                    var s = org.opalj.collection.immutable.GrowableIntTrieSet.empty
                    for { i ← 0 to 10000 } {
                        s += rngGen.nextInt()
                    }
                    s
                }
            } { mu ⇒ info(s"required $mu bytes") }

            var total = 0L
            PerformanceEvaluation.time {
                for { set ← allSets; v ← set } {
                    total += v
                }
            } { t ⇒ info(s"${t.toSeconds} for foreach") }

            info(s"overall size: ${allSets.map(_.size).sum}; sum: $total")
        }
    }
}
