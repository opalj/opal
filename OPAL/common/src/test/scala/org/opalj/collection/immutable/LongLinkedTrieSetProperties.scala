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

    property("equals and hashCode") = forAll { s: IntArraySet ⇒
        val its1 = s.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        val its2 = s.foldLeft(EmptyLongLinkedTrieSet: LongLinkedTrieSet)(_ + _.toLong)
        its1 == its2 &&
            its1.hashCode() == its2.hashCode()
    }

}

@RunWith(classOf[JUnitRunner])
class LongLinkedTrieSetTest extends FunSpec with Matchers {

    describe("contains") {

        it("[regression] should return true for all values of the test fixture:"+
            "(-103806,-99428,-15784,-6124,48020)") {
            val d = List[Long](-103806, -99428, -15784, -6124, 48020)

            val llts = d.foldLeft(LongLinkedTrieSet.empty)((c, n) ⇒ c + n)
            var notFound = List.empty[Long]
            d.foreach(v ⇒ if (!llts.contains(v)) notFound ::= v)
            if (notFound.nonEmpty)
                fail(s"; lookup of ${notFound.head}(${notFound.head.toBinaryString}) failed: $llts")
        }

        it("[regression] should return true for all values of the test fixture:"+
            "(-134206, -128016, -124763, -106014, -99624, -97374, -90508, -79349, -77213,"+
            "-20404, 4063, 6348, 14217, 21395, 23943, 25328, 30684, 33875)") {
            // we had a regression given the follwing set
            val d = List[Long](
                -134206, -128016, -124763, -106014, -99624, -97374, -90508, -79349, -77213,
                -20404, 4063, 6348, 14217, 21395, 23943, 25328, 30684, 33875
            )

            val llts = d.foldLeft(LongLinkedTrieSet.empty)((c, n) ⇒ c + n)
            var notFound = List.empty[Long]
            d.foreach(v ⇒ if (!llts.contains(v)) notFound ::= v)
            if (notFound.nonEmpty)
                fail(s"; lookup of ${notFound.head}(${notFound.head.toBinaryString}) failed: $llts")
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
}