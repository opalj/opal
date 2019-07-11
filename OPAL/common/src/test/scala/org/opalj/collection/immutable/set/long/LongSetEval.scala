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

abstract class LongSetEval extends App {

            it("when comparing with Set[Long]") {
                val seed = 123456789L
                val rngGen = new java.util.Random(seed)

                var opalS = org.opalj.collection.immutable.LongTrieSet.empty
                var scalaS = Set.empty[Long]
                for { i ← 0 to 1000000 } {
                    val v = rngGen.nextLong()
                    opalS += v
                    scalaS += v
                }

                var opalTotal = 0L
                PerformanceEvaluation.time {
                    for { v ← opalS } { opalTotal += v }
                } { t ⇒ info(s"OPAL ${t.toSeconds} for foreach") }

                var scalaTotal = 0L
                PerformanceEvaluation.time {
                    for { v ← scalaS } { scalaTotal += v }
                } { t ⇒ info(s"Scala ${t.toSeconds} for foreach") }

                assert(opalTotal == scalaTotal, s"$opalS vs. $scalaS")
            }
        }

        it("for small sets (up to 8 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i ← 1 to 3 } rngGen.nextLong();
            val setValues = (for { i ← 1 to 1000 } yield Math.abs(rngGen.nextLong())).toArray
            val queryValues = (for { i ← 1 to 1000 } yield Math.abs(rngQuery.nextLong())).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 10000000 } {
                    var s = org.opalj.collection.immutable.LongTrieSet.empty
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
            for { i ← 1 to 3 } rngGen.nextLong();
            val setValues = (for { i ← 1 to 1000 } yield Math.abs(rngGen.nextLong())).toArray
            val queryValues = (for { i ← 1 to 1000 } yield Math.abs(rngQuery.nextLong())).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 10000000 } {
                    var s = org.opalj.collection.immutable.LongTrieSet.empty
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
            for { i ← 1 to 16 } rngGen.nextLong();
            val setValues = (for { i ← 1 to 10000 } yield Math.abs(rngGen.nextLong())).toArray
            val queryValues = (for { i ← 1 to 10000 } yield Math.abs(rngQuery.nextLong())).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 1000000 } {
                    var s = org.opalj.collection.immutable.LongTrieSet.empty
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
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i ← 1 to 3333 } rngGen.nextLong();
            val setValues = (for { i ← 1 to 10000 } yield rngGen.nextLong()).toArray
            val queryValues = (for { i ← 1 to 10000 } yield rngQuery.nextLong()).toArray

            PerformanceEvaluation.time {
                for { runs ← 0 until 10000 } {
                    var s = org.opalj.collection.immutable.LongTrieSet.empty
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
                    var s = org.opalj.collection.immutable.LongTrieSet.empty
                    for { i ← 0 to 10000 } {
                        s += rngGen.nextLong()
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
