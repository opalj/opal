/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FunSpec

import org.opalj.util.PerformanceEvaluation

object LongLinkedTrieSetProperties extends Properties("LongLinkedTrieSet") {

    val r = new java.util.Random()

    val smallListsGen = for { m ← Gen.listOfN(8, Arbitrary.arbitrary[Long]) } yield (m)

    implicit val arbIntArraySet: Arbitrary[IntArraySet] = Arbitrary {
        Gen.sized { l ⇒
            val s = (0 until l).foldLeft(IntArraySet.empty) { (c, n) ⇒
                val nextValue = r.nextInt(75000) - 25000
                c + (if (n % 2 == 0) nextValue else -nextValue - 100000)
            }
            s
        }
    }

    implicit val arbListOfIntArraySet: Arbitrary[List[IntArraySet]] = Arbitrary {
        Gen.sized { s ⇒ Gen.listOfN(s, Arbitrary.arbitrary[IntArraySet]) }
    }

    // NOT TO BE USED BY TESTS THAT TEST THE CORRECT CONSTRUCTION!
    implicit val arbLongTrieSet: Arbitrary[LongTrieSet] = Arbitrary {
        Gen.sized { l ⇒
            (0 until l).foldLeft(LongTrieSet.empty) { (c, n) ⇒
                val nextValue = Math.abs(r.nextLong()) / 75000 - 25000
                c + (if (n % 2 == 0) nextValue else -nextValue - 100000)
            }
        }
    }

    implicit val arbPairOfIntArraySet: Arbitrary[(IntArraySet, IntArraySet)] = Arbitrary {
        Gen.sized { l ⇒
            Gen.sized { j ⇒
                (
                    (0 until l).foldLeft(IntArraySet.empty) { (c, n) ⇒ c + r.nextInt(100) - 50 },
                    (0 until j).foldLeft(IntArraySet.empty) { (c, n) ⇒ c + r.nextInt(100) - 50 }
                )
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create singleton LongLinkedTrieSet") = forAll { v: Long ⇒
        val factoryITS = LongLinkedTrieSet1(v)
        val viaEmptyITS = EmptyLongLinkedTrieSet + v
        factoryITS.size == 1 &&
            factoryITS.isSingletonSet &&
            !factoryITS.isEmpty &&
            factoryITS.head == v &&
            viaEmptyITS.head == v &&
            viaEmptyITS.size == 1
    }

    property("create LongLinkedTrieSet from Set step by step") = forAll { s: Set[Long] ⇒
        val l = s.toList
        val its = l.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        (its.size == s.size) :| "matching size" &&
            (its.iterator.mkString(",") == l.reverse.mkString(",")) :| "same content"
    }

    property("contains") = forAll { (s1: IntArraySet, s2: IntArraySet) ⇒
        val its = s1.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        s1.forall(i ⇒ its.contains(i.toLong)) :| "contains expected value" &&
            s2.forall(v ⇒ s1.contains(v) == its.contains(v.toLong))
    }

    property("foreach") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        var newS = IntArraySet.empty
        its foreach { newS += _.toInt } // use foreach to compute a new set
        s == newS
    }

    /*
    property("equals and hashCode") = forAll { s: IntArraySet ⇒
        val its1 = s.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        val its2 = s.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        its1 == its2 &&
            its1.hashCode() == its2.hashCode()
    }*/

}

@RunWith(classOf[JUnitRunner])
class LongLinkedTrieSetTest extends FunSpec with Matchers {

    describe("contains") {

        val fixtures = List[List[Long]](
            List[Long](-92276, -76687, -1003, 39908),
            List[Long](-149831, -143246, -110997, -103241, -100192, -91362, -14553, -10397, -2126, -628, 8184, 13255, 39973),
            List[Long](-103806, -99428, -15784, -6124, 48020),
            List[Long](-134206, -128016, -124763, -106014, -99624, -97374, -90508, -79349, -77213, -20404, 4063, 6348, 14217, 21395, 23943, 25328, 30684, 33875)
        )

        for { fixture ← fixtures } {
            it("[regression] should return true for all values of the test fixture: "+fixture) {
                val llts = fixture.foldLeft(LongLinkedTrieSet.empty)((c, n) ⇒ c + n)
                var notFound = List.empty[Long]
                fixture.foreach(v ⇒ if (!llts.contains(v)) notFound ::= v)
                if (notFound.nonEmpty)
                    fail(s"lookup of ${notFound.head}(${notFound.head.toBinaryString}) failed: $llts")
            }

        }

        it("for sets with four values with many leading zeros") {
            val ls = org.opalj.collection.immutable.LongLinkedTrieSet1(128L) + 16L + 32L + 64L
            assert(ls.contains(128L))
            assert(ls.contains(16L))
            assert(ls.contains(32L))
            assert(ls.contains(64L))

            assert(!ls.contains(8L))
            assert(!ls.contains(-16L))
            assert(!ls.contains(15L))
            assert(!ls.contains(0L))
            assert(!ls.contains(1L))
            assert(!ls.contains(2L))
        }

        it("for sets with four values with many leading ones") {
            val ls = org.opalj.collection.immutable.LongLinkedTrieSet1(7L) + 15L + 31L + 63L
            assert(ls.contains(7L))
            assert(ls.contains(15L))
            assert(ls.contains(31L))
            assert(ls.contains(63L))

            assert(!ls.contains(128L))
            assert(!ls.contains(16L))
            assert(!ls.contains(32L))
            assert(!ls.contains(64L))
            assert(!ls.contains(8L))
            assert(!ls.contains(-16L))
            assert(!ls.contains(0L))
            assert(!ls.contains(1L))
            assert(!ls.contains(2L))
        }
    }

    describe("performance") {

        it("for small sets (up to 6 elements) creation and contains check should finish in reasonable time (all values are positive)") {
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
                    var s = org.opalj.collection.immutable.LongLinkedTrieSet.empty
                    var hits = 0
                    for { i ← 0 until rngGen.nextInt(7) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t ⇒ info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for small sets (up to 24 elements) creation and contains check should finish in reasonable time (all values are positive)") {
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
                    var s = org.opalj.collection.immutable.LongLinkedTrieSet.empty
                    var hits = 0
                    for { i ← 0 until rngGen.nextInt(25) } {
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
                    var s = org.opalj.collection.immutable.LongLinkedTrieSet.empty
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

        it("memory usage") {
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)

            val allSets = PerformanceEvaluation.memory {
                for {
                    set ← 0 until 2500
                } yield {
                    var s = org.opalj.collection.immutable.LongLinkedTrieSet.empty
                    for { i ← 0 until 10000 } {
                        s += rngGen.nextLong()
                    }
                    s
                }
            } { mu ⇒ info(s"required $mu bytes for 1000 sets with ~10000 elements each") }
            info(s"overall size: ${allSets.map(_.size).sum}")
        }
    }
}