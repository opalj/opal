/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatest.Matchers
import org.scalatest.FunSpec

import org.opalj.util.PerformanceEvaluation

/**
 * Tests `LongTrieSet` by creating a standard Scala Set and comparing
 * the results of the respective functions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object LongTrieSetProperties extends Properties("LongTrieSet") {

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

    property("foreach") = forAll { s: IntArraySet ⇒
        val its = s.iterator.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        var newS = IntArraySet.empty
        its foreach { newS += _.toInt } // use foreach to compute a new set
        s == newS
    }

    property("create singleton LongTrieSet") = forAll { v: Long ⇒
        val factoryITS = LongTrieSet1(v)
        val viaEmptyITS = EmptyLongTrieSet + v
        factoryITS.size == 1 &&
            factoryITS.isSingletonSet &&
            !factoryITS.isEmpty &&
            factoryITS.head == v &&
            viaEmptyITS.hashCode == factoryITS.hashCode &&
            viaEmptyITS == factoryITS
    }

    /*
    property("create LongTrieSet from Set step by step") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _.toLong)
        (its.size == s.size) :| "matching size" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toList.sorted == s.iterator.toList.sorted) :| "same content"
    }
   */

    property("+ (adding) values to a set which are already in the set will result in the same set") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _.toLong)
        (its.size == s.size) :| "matching size" &&
            (its eq (s.foldLeft(its)(_ + _.toLong)))
    }

    /*
    property("create LongTrieSet from List (i.e., with duplicates)") = forAll { l: List[Long] ⇒
        val its = l.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _)
        val lWithoutDuplicates = l.toSet.toList
        (its.size == lWithoutDuplicates.size) :| "matching size" &&
            its.iterator.toList.sorted == lWithoutDuplicates.sorted
    }*/

    property("contains") = forAll { (s1: IntArraySet, s2: IntArraySet) ⇒
        val its = s1.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _.toLong)
        s1.forall(i ⇒ its.contains(i.toLong)) :| "contains expected value" &&
            s2.forall(v ⇒ s1.contains(v) == its.contains(v.toLong))
    }

    /*

    property("mkString(String,String,String)") = forAll { (s: Set[Long], pre: String, in: String, post: String) ⇒
        val its = s.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _)
        val itsString = its.mkString(pre, in, post)
        val sString = its.iterator.mkString(pre, in, post)
        val lString = s.mkString(pre, in, post)
        ((itsString.length == sString.length) :| "length of generated string (its vs s)") &&
            ((itsString.length == lString.length) :| "length of generated string (its vs l)") &&
            itsString.startsWith(pre) && itsString.endsWith(post)
    }

    property("mkString(String)") = forAll { (s: Set[Long], in: String) ⇒
        val its = s.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _)
        val itsString = its.mkString(in)
        val sString = its.iterator.mkString(in)
        val lString = s.mkString(in)
        ((itsString.length == sString.length) :| "length of generated string (its vs s)") &&
            ((itsString.length == lString.length) :| "length of generated string (its vs l)")
    }


    property("map") = forAll { s: IntArraySet ⇒
        val its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        val mappedIts = its.map(_ * 2)
        val mappedS = s.map(_ * 2)
        classify(mappedIts.size > 3, "using trie") {
            mappedS.size == mappedIts.size &&
                (EmptyLongTrieSet ++ mappedS.iterator.map(_.toLong)) == mappedIts
        }
    }

    property("transform") = forAll { s: LongTrieSet ⇒
        val its = s.transform(_ * 2, Chain.newBuilder[Long]).toIterator.toList.sorted
        its == s.map(_ * 2).iterator.toList.sorted
    }

    property("exists") = forAll { s: IntArraySet ⇒
        val its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        classify(its.isEmpty, "the set is empty") {
            s.forall(v ⇒ its.exists(_ == v)) &&
                s.forall(v ⇒ its.exists(_ != v) == s.exists(_ != v))
        }
    }

    property("forall") = forAll { s: IntArraySet ⇒
        val its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        its.forall(l ⇒ s.contains(l.toInt)) &&
            its.forall(v ⇒ s.contains(-v.toInt)) == s.forall(v ⇒ s.contains(-v))
    }

    property("foldLeft") = forAll { s: IntArraySet ⇒
        val its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        its.foldLeft(0L)(_ + _) == s.foldLeft(0L)(_ + _)
    }

    property("flatMap") = forAll { listOfSets: List[IntArraySet] ⇒
        listOfSets.nonEmpty ==> {
            val arrayOfSets = listOfSets.toArray
            val arrayOfLTSets: Array[LongTrieSet] = arrayOfSets.map(s ⇒ LongTrieSet.empty ++ s.iterator.map(_.toLong))
            val l = arrayOfSets.length

            val flatMappedLTSet = arrayOfLTSets(0).flatMap(v ⇒ arrayOfLTSets(Math.abs(v).toInt % l))
            val flatMappedSSet = arrayOfSets(0).flatMap(v ⇒ arrayOfSets(Math.abs(v) % l))

            classify(flatMappedSSet.size > 50, "set with more than 50 elements") {
                classify(flatMappedSSet.size < listOfSets.map(_.size).sum, "flat map is not the join of all sets") {
                    flatMappedSSet.forall(i ⇒ flatMappedLTSet.contains(i.toLong)) &&
                        flatMappedSSet.size == flatMappedLTSet.size
                }
            }
        }
    }

    property("not equals") = forAll { s: IntArraySet ⇒
        val its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        its != (new Object)
    }

    property("equals") = forAll { s: LongTrieSet ⇒
        val i = { var i = 0L; while (s.contains(i)) i += 1L; i }
        val newS = (s + i - i)
        s == newS && s.hashCode == newS.hashCode
    }

    property("toString") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(LongTrieSet.empty)(_ +! _.toLong)
        val itsToString = its.toString
        itsToString.startsWith("LongTrieSet(") && itsToString.endsWith(")")
        // IMPROVE add content based test
    }

   */
}

@RunWith(classOf[JUnitRunner])
class LongTrieSetTest extends FunSpec with Matchers {

    describe("create an LongTrieSet from four values") {

        it("should contain all values if the values are distinct") {
            assert(LongTrieSet(1, 2, 3, 4).size == 4)
            assert(LongTrieSet(256, 512, 1024, 2048).size == 4)
            assert(LongTrieSet(0, 1, 10, 1000000).size == 4)
            assert(LongTrieSet(1110, 11, 10, 1).size == 4)
        }

        it("should contain only three values if two values are equal") {
            assert(LongTrieSet(1, 2, 3, 2).size == 3)
            assert(LongTrieSet(1, 1, 3, 2).size == 3)
            assert(LongTrieSet(1, 2, 3, 3).size == 3)
            assert(LongTrieSet(1, 2, 3, 1).size == 3)
        }

        it("should contain only two values if three values are equal") {
            assert(LongTrieSet(1, 2, 2, 2).size == 2)
            assert(LongTrieSet(1, 1, 2, 1).size == 2)
            assert(LongTrieSet(1, 2, 2, 2).size == 2)
            assert(LongTrieSet(2, 2, 2, 1).size == 2)
            assert(LongTrieSet(2, 2, 1, 2).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(LongTrieSet(2, 2, 2, 2).size == 1)
        }
    }

    describe("create an LongTrieSet from three values") {

        it("should contain all values if the values are distinct") {
            assert(LongTrieSet(1, 2, 4).size == 3)
            assert(LongTrieSet(256, 1024, 2048).size == 3)
            assert(LongTrieSet(0, 1, 1000000).size == 3)
            assert(LongTrieSet(1110, 11, 1).size == 3)
        }

        it("should contain only two values if two values are equal") {
            assert(LongTrieSet(1, 2, 2).size == 2)
            assert(LongTrieSet(1, 1, 2).size == 2)
            assert(LongTrieSet(1, 2, 2).size == 2)
            assert(LongTrieSet(2, 1, 2).size == 2)
            assert(LongTrieSet(2, 2, 1).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(LongTrieSet(2, 2, 2).size == 1)
        }
    }

    describe("create an LongTrieSet from two values") {

        it("should contain all values if the values are distinct") {
            assert(LongTrieSet(1, 2).size == 2)
            assert(LongTrieSet(256, 2048).size == 2)
            assert(LongTrieSet(0, 1000000).size == 2)
            assert(LongTrieSet(1110, 11).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(LongTrieSet(2, 2).size == 1)
        }
    }

    describe("regression tests") {
        it("should implement contains correctly") {
            val s = LongTrieSet(-149916L, -102540L, -118018L) + -91539L + 0L
            assert(s.contains(-149916L))
            assert(s.contains(-102540L))
            assert(s.contains(-118018L))
            assert(s.contains(-91539L))
            assert(s.contains(0L))

        }
    }

    describe("processing a LongTrieSet which is leaning to the right") {

        val s = LongTrieSet(8192) + 16384 + 32768 + 65536 + 131072

        it("it should contain the given values") {
            assert(s.contains(8192))
            assert(s.contains(16384))
            assert(s.contains(32768))
            assert(s.contains(65536))
            assert(s.contains(131072))
        }
    }

    describe("performance") {

        describe("contains") {

            val fixtures = List[List[Long]](
                List[Long](4414074060632414370L, 1896250972871104879L, -4468262829510781048L, 3369759390166412338L, 3433954040001057900L, -5360189778998759153L, -4455613594770698331L, 7795367189183618087L, 7342745861545843810L, -938149705997478263L, -7298104853677454976L, 4601242874523109082L, 4545666121642261549L, 2117478629717484238L),
                List[Long](-92276, -76687, -1003, 39908),
                List[Long](-149831, -143246, -110997, -103241, -100192, -91362, -14553, -10397, -2126, -628, 8184, 13255, 39973),
                List[Long](-103806, -99428, -15784, -6124, 48020),
                List[Long](-134206, -128016, -124763, -106014, -99624, -97374, -90508, -79349, -77213, -20404, 4063, 6348, 14217, 21395, 23943, 25328, 30684, 33875)
            )

            for { fixture ← fixtures } {
                it("[regression] should return true for all values of the test fixture: "+fixture) {
                    val llts = fixture.foldLeft(LongTrieSet.empty)((c, n) ⇒ c + n)
                    var notFound = List.empty[Long]
                    fixture.foreach(v ⇒ if (!llts.contains(v)) notFound ::= v)
                    if (notFound.nonEmpty)
                        fail(s"lookup of ${notFound.head}(${notFound.head.toBinaryString}) failed: $llts")
                }

            }

            it("for sets with four values with many leading zeros") {
                val ls = org.opalj.collection.immutable.LongTrieSet1(128L) + 16L + 32L + 64L
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
                val ls = org.opalj.collection.immutable.LongTrieSet1(7L) + 15L + 31L + 63L
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
