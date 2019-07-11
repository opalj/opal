/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set
package long

import org.opalj.util.PerformanceEvaluation

abstract class LongSetEval {

    final val numberOfSetsWithSize3orLess = 10000000

    def eval[T](
        empty:    () ⇒ T,
        size:     (T) ⇒ Int,
        add:      (T) ⇒ Long ⇒ T,
        contains: (T) ⇒ Long ⇒ Boolean
    ): Unit = {

        val rngSeed = 123456789L
        val rngGen = new java.util.Random(rngSeed)

        def createSets(maxSize: Int, numberOfSets: Int): Unit = {
            println(s"creating $numberOfSets sets with $maxSize elements...")
            val allSets = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    var r = List.empty[T]
                    var i = 0
                    do {
                        var l = 0
                        var s = empty()
                        do {
                            s = add(s)(rngGen.nextLong)
                            l += 1
                        } while (l < maxSize)
                        r ::= s
                        i += 1
                    } while (i < numberOfSets)
                    r
                } { t ⇒ println(s"\ttook: ${t.toSeconds}") }
            } { m ⇒ println(s"\trequired: ${m / 1024 / 1024} MB") }
            println(s"\tand stores: ${allSets.map(size(_)).sum} elements")
        }

        createSets(maxSize = 3, numberOfSetsWithSize3orLess)
        createSets(maxSize = 16, numberOfSetsWithSize3orLess / 10)
        createSets(maxSize = 5000, numberOfSetsWithSize3orLess / 1000)
    }

    /*
            it("when comparing with Set[Long]") {


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




    describe(s"performance") {

        describe(s"performance") {

            it("when comparing with Set[Long]") {
                val opalS = PerformanceEvaluation.memory {
                    PerformanceEvaluation.time {
                        val seed = 123456789L
                        val rngGen = new java.util.Random(seed)
                        var opalS = LongTrieSet.empty
                        for { i ← 0 to 1000000 } {
                            val v = rngGen.nextLong()
                            opalS += v
                        }
                        opalS
                    } { t ⇒ info(s"LongTrieSet took ${t.toSeconds}") }
                } { mu ⇒ info(s"LongTrieSet required $mu bytes") }

                val scalaS = PerformanceEvaluation.memory {
                    PerformanceEvaluation.time {
                        val seed = 123456789L
                        val rngGen = new java.util.Random(seed)
                        var scalaS = Set.empty[Long]
                        for { i ← 0 to 1000000 } {
                            val v = rngGen.nextLong()
                            scalaS += v
                        }
                        scalaS
                    } { t ⇒ info(s"Set[Long] took ${t.toSeconds}") }
                } { mu ⇒ info(s"Set[Long] required $mu bytes") }

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
                    var s = LongTrieSet.empty
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
                    var s = LongTrieSet.empty
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
                    var s = LongTrieSet.empty
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
            for { i ← 1 to 3333 } rngGen.nextLong();
            val setValues = (for { i ← 1 to 10000 } yield rngGen.nextLong()).toArray
            val queryValues = (for { i ← 1 to 10000 } yield rngQuery.nextLong()).toArray

            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            PerformanceEvaluation.time {
                for { runs ← 0 until 10000 } {
                    var s = LongTrieSet.empty
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
                    var s = LongTrieSet.empty
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
    */

}
