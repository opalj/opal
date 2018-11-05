/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatest.Matchers
import org.scalatest.FunSpec

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

    property("create singleton LongTrieSet") = forAll { v: Long ⇒
        val factoryITS = LongTrieSet1(v)
        val viaEmptyITS = EmptyLongTrieSet + v
        factoryITS.size == 1 &&
            factoryITS.isSingletonSet &&
            !factoryITS.isEmpty &&
            !factoryITS.hasMultipleElements &&
            factoryITS.head == v &&
            viaEmptyITS.hashCode == factoryITS.hashCode &&
            viaEmptyITS == factoryITS
    }

    property("create LongTrieSet from Set step by step") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _.toLong)
        (its.size == s.size) :| "matching size" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.hasMultipleElements == (s.size > 1)) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toList.sorted == s.iterator.toList.sorted) :| "same content"
    }

    property("create LongTrieSet from Set[Long] (TraversableOnce) using ++") = forAll { s: Set[Long] ⇒
        val its = EmptyLongTrieSet ++ s
        (its.size == s.size) :| "matching size" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.hasMultipleElements == (s.size > 1)) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toList.sorted == s.iterator.toList.sorted) :| "same content"
    }

    property("create LongTrieSet from List (i.e., with duplicates)") = forAll { l: List[Long] ⇒
        val its = l.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _)
        val lWithoutDuplicates = l.toSet.toList
        (its.size == lWithoutDuplicates.size) :| "matching size" &&
            its.iterator.toList.sorted == lWithoutDuplicates.sorted
    }

    property("head") = forAll { s: LongTrieSet ⇒
        s.nonEmpty ==> {
            val its = s.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _)
            s.contains(its.head)
        }
    }

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

    property("contains") = forAll { (s1: IntArraySet, s2: IntArraySet) ⇒
        val its = s1.foldLeft(EmptyLongTrieSet: LongTrieSet)(_ + _.toLong)
        s1.forall(i ⇒ its.contains(i.toLong)) :| "contains expected value" &&
            s2.forall(v ⇒ s1.contains(v) == its.contains(v.toLong))
    }

    property("foreach") = forAll { s: IntArraySet ⇒
        val its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        var newS = IntArraySet.empty
        its foreach { newS += _.toInt } // use foreach to compute a new set
        s == newS
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

    property("headAndTail") = forAll { s: IntArraySet ⇒
        var its = EmptyLongTrieSet ++ s.iterator.map(_.toLong)
        var removed = Chain.empty[Long]
        while (its.nonEmpty) {
            val LongRefPair(v, newIts) = its.headAndTail
            removed :&:= v
            its = newIts
        }
        (removed.toIterator.toSet.size == s.size) :| "no value is returned more than once" &&
            (removed.size == s.size) :| "all values are returned"
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

    property("+!") = forAll { s: LongTrieSet ⇒
        val its = EmptyLongTrieSet ++! s
        its == s
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

    property("subsetOf (similar)") = forAll { (s1: IntArraySet, i: Long) ⇒
        val its1 = s1.foldLeft(LongTrieSet.empty)(_ +! _.toLong)
        val its2 = s1.foldLeft(LongTrieSet.empty)(_ +! _.toLong) +! i
        val setSubsetOf = its1.subsetOf(its2)
        val iteratorSubsetOf = its1.iterator.toSet.subsetOf(its2.iterator.toSet)
        classify(its1.size == 0, "its1 is empty") {
            classify(its1.size == 1, "its1.size == 1") {
                classify(its1.size == 2, "its1.size == 2") {
                    classify(its1.size == its2.size, "its1 == its2") {
                        (setSubsetOf == iteratorSubsetOf) :| s"$setSubsetOf == $iteratorSubsetOf"
                    }
                }
            }
        }
    }

    property("subsetOf (always succeeding)") = forAll { (s1: IntArraySet, s2: IntArraySet) ⇒
        val its1 = s1.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val its2 = s2.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val mergedIts = its1 ++ its2
        classify(its1.size == mergedIts.size, "its1 and its1 merged with its2 are different") {
            its1.subsetOf(mergedIts) && its2.subsetOf(mergedIts)
        }
    }

    property("filter (identity if no value is filtered)") = forAll { s: LongTrieSet ⇒
        val i = { var i = 0L; while (s.contains(i)) i += 1L; i }
        s.filter(_ != i) eq s
    }

    property("filter") = forAll { (s1: IntArraySet, s2: IntArraySet) ⇒
        val its1 = s1.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val its2 = s2.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val newits = its1.filter(v ⇒ !its2.contains(v))
        val news = s1.withFilter(v ⇒ !s2.contains(v))
        classify(news.size < s1.size, "filtered something") {
            news.forall(v ⇒ newits.contains(v.toLong)) && newits.forall(l ⇒ news.contains(l.toInt))
        }
    }

    property("-") = forAll { (ps: (IntArraySet, IntArraySet)) ⇒
        val (s, other) = ps
        val its = s.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val newits = other.foldLeft(its)(_ - _.toLong)
        val news = other.foldLeft(s)(_ - _)
        classify(news.size < s.size, "removed something") {
            (its.size == s.size) :| "the original set is unmodified" &&
                news.forall(i ⇒ newits.contains(i.toLong)) && newits.forall(l ⇒ news.contains(l.toInt))
        }
    }

    property("intersect") = forAll { (ps: (IntArraySet, IntArraySet)) ⇒
        val (ias1, ias2) = ps
        val s1 = ias1.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val s2 = ias2.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val expected = s1.foldLeft(LongTrieSet.empty)((c, n) ⇒ if (s2.contains(n)) c + n else c)
        (s1 intersect s2) == expected &&
            (s2 intersect s1) == expected
    }

    property("- (all elements)") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val newits = s.foldLeft(its)(_ - _.toLong)
        newits.size == 0 && newits.isEmpty &&
            newits == EmptyLongTrieSet
    }

    property("filter (all elements)") = forAll { s: IntArraySet ⇒
        val its = s.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        its.filter(i ⇒ false) eq EmptyLongTrieSet
    }

    property("withFilter") = forAll { ss: (IntArraySet, IntArraySet) ⇒
        val (s1: IntArraySet, s2: IntArraySet) = ss
        val its1 = s1.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        val its2 = s2.foldLeft(LongTrieSet.empty)(_ + _.toLong)
        var evaluated = false
        val newits = its1.withFilter(i ⇒ { evaluated = true; !its2.contains(i) })
        val news = s1.withFilter(!s2.contains(_))
        !evaluated :| "not eagerly evaluated" &&
            news.forall(i ⇒ newits.exists(newi ⇒ newi == i)) :| "exists check" &&
            (news.forall(i ⇒ newits.contains(i.toLong)) && newits.forall(l ⇒ news.contains(l.toInt))) :| "contains check" &&
            news.forall(i ⇒ newits.iterator.contains(i.toLong)) :| s"iterator.contains $news vs. $newits" &&
            newits.iterator.forall(l ⇒ news.contains(l.toInt)) :| "iterator.forall" &&
            news.forall(i ⇒ newits.iterator.contains(i.toLong)) && newits.iterator.forall(l ⇒ news.contains(l.toInt)) :| "LongIterator"
    }

}

@RunWith(classOf[JUnitRunner])
class LongTrieSetTest extends FunSpec with Matchers {

    describe("the subset of relation") {
        it("should correctly work for empty set related comparisons") {
            assert(LongTrieSet.empty.subsetOf(LongTrieSet.empty))
            assert((LongTrieSet(1) - 1).subsetOf(LongTrieSet.empty))
            assert((LongTrieSet(1) - 1).subsetOf(LongTrieSet(1)))
            assert((LongTrieSet(1, 2, 3) - 1 - 2 - 3).subsetOf(LongTrieSet(1)))

            assert(LongTrieSet.empty.subsetOf(LongTrieSet(1)))
            assert(LongTrieSet.empty.subsetOf(LongTrieSet(1, 2)))
            assert(LongTrieSet.empty.subsetOf(LongTrieSet(1, 2, 3)))
            assert(LongTrieSet.empty.subsetOf(LongTrieSet(1, 3, 4, 5)))

        }

        it("should correctly work for set1 related comparisons") {
            assert(LongTrieSet.empty.subsetOf(LongTrieSet1(1)))
            assert(!LongTrieSet(2).subsetOf(LongTrieSet.empty))

            assert(LongTrieSet(1).subsetOf(LongTrieSet(1)))
            assert(!LongTrieSet(2).subsetOf(LongTrieSet(3)))

            assert(LongTrieSet(1).subsetOf(LongTrieSet(1, 2)))
            assert(LongTrieSet(2).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(3).subsetOf(LongTrieSet(1, 2)))

            assert(LongTrieSet(1).subsetOf(LongTrieSet(1, 2, 3)))
            assert(LongTrieSet(2).subsetOf(LongTrieSet(1, 2, 3)))
            assert(LongTrieSet(3).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(4).subsetOf(LongTrieSet(1, 2, 3)))

            assert(LongTrieSet(1).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(2).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(3).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(5).subsetOf(LongTrieSet(1, 2, 3, 4)))
        }

        it("should correctly work for set2 related comparisons") {
            assert(!LongTrieSet(1, 2).subsetOf(LongTrieSet.empty))
            assert(!LongTrieSet(1, 2).subsetOf(LongTrieSet(1)))

            assert(LongTrieSet(1, 2).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(2, 3).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(1, 3).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(-1, 1).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(-1, 2).subsetOf(LongTrieSet(1, 2)))

            assert(LongTrieSet(1, 2).subsetOf(LongTrieSet(1, 2, 3)))
            assert(LongTrieSet(1, 3).subsetOf(LongTrieSet(1, 2, 3)))
            assert(LongTrieSet(2, 3).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(-1, 2).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(1, 4).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(2, 4).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(3, 4).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(0, 4).subsetOf(LongTrieSet(1, 2, 3)))

            assert(LongTrieSet(1, 2).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(1, 3).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(1, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(2, 3).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(2, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(3, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(-1, 2).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(1, 5).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(2, 5).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(3, 5).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(4, 5).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(0, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
        }

        it("should correctly work for set3 related comparisons") {
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet.empty))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(2)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(3)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(0)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(2, 3)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 3)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 5)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(0, 5)))

            assert(LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2, 4)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(2, 3, 4)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 3, 4)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2, 0)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(2, 3, 0)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 3, 0)))

            assert(LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(1, 2, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(1, 3, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(LongTrieSet(2, 3, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2, 4, 5)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(2, 3, 4, 5)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 3, 4, 5)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 2, 0, 5)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(2, 3, 0, 5)))
            assert(!LongTrieSet(1, 2, 3).subsetOf(LongTrieSet(1, 3, 0, 5)))
        }

        it("should correctly work for set4 related comparisons") {
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet.empty))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(3)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(3, 4)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(0, 5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 3)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 4)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3, 4)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3, 4)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3, 0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3, 0)))

            assert(LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 3, 4)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 4, 5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3, 4, 5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 3, 5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3, 4, 5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 4, 0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3, 4, 0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 3, 0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3, 4, 0)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 4, -5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3, 4, -5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 3, -5)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3, 4, -2)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 4, -2)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 3, 4, -2)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(1, 2, 3, -2)))
            assert(!LongTrieSet(1, 2, 3, 4).subsetOf(LongTrieSet(2, 3, 4, -2)))
        }

        it("should correctly work for set5+ related comparisons") {

            // the following are derived from failing tests...

            val s8a = LongTrieSet(-132967, -114794, -91882, -87887) + (-23059) + (-16654) + (-623) + 35514
            val s8b = LongTrieSet(-90376, -83774, -82124, -81207) + (-18991) + (-5490) + 11406 + 11506
            assert(!s8a.subsetOf(s8b))
            assert(!s8b.subsetOf(s8a))

            val s18 = LongTrieSet.empty ++ List[Long](-127543, -104227, -103908, -103694, -100767, -90387, -86807, -80533, -78983, -14063, -10431, -10212, -6447, -298, 163, 9627, 19840, 38723)
            val s18_plus_m1 = s18 + (-1L)
            assert(s18.subsetOf(s18_plus_m1), s"$s18 expected to be subset of $s18_plus_m1")

            val s4 = LongTrieSet(-149916, -118018, -102540, -91539)
            val s4_plus_0 = s4 + 0
            assert(s4.subsetOf(s4_plus_0), s"$s4 expected to be subset of $s4_plus_0")
        }
    }

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

    describe("create an LongTrieSet from one value") {
        it("should contain the value") {
            assert(LongTrieSet(1).head == 1)
        }
    }

    describe("regression tests") {
        it("should implement contains correctly") {
            val s = LongTrieSet(-149916, -102540, -118018, -91539) + 0
            assert(s.contains(-149916))
            assert(s.contains(-102540))
            assert(s.contains(-118018))
            assert(s.contains(-91539))
            assert(s.contains(0))

        }
    }

    describe("processing an LongTrieSet which is leaning to the right") {

        val s = LongTrieSet(8192) + 16384 + 32768 + 65536 + 131072

        it("it should contain the given values") {
            assert(s.contains(8192))
            assert(s.contains(16384))
            assert(s.contains(32768))
            assert(s.contains(65536))
            assert(s.contains(131072))
        }

        it("it should be able to eagerly filter the values") {
            val s1 = s.filter(_ != 8192)
            assert(!s1.contains(8192))
            assert(s1.contains(16384))
            assert(s1.contains(32768))
            assert(s1.contains(65536))
            assert(s1.contains(131072))

            val s2 = s1.filter(_ != 32768)
            assert(s2.contains(16384))
            assert(!s2.contains(32768))
            assert(s2.contains(65536))
            assert(s2.contains(131072))

            val s3 = s2.filter(_ != 131072)
            assert(s3.contains(16384))
            assert(s3.contains(65536))
            assert(!s3.contains(131072))
        }

        it("it should be possible to lazily filter the values step by step") {
            val s1 = s.withFilter(_ != 8192)
            assert(!s1.contains(8192))
            assert(s1.contains(16384))
            assert(s1.contains(32768))
            assert(s1.contains(65536))
            assert(s1.contains(131072))

            val s2 = s1.withFilter(_ != 32768)
            assert(!s2.contains(8192))
            assert(s2.contains(16384))
            assert(!s2.contains(32768))
            assert(s2.contains(65536))
            assert(s2.contains(131072))

            val s3 = s2.withFilter(_ != 131072)
            assert(!s3.contains(8192))
            assert(s3.contains(16384))
            assert(!s3.contains(32768))
            assert(s3.contains(65536))
            assert(!s3.contains(131072))
        }
    }

    describe("filtering a LongTrieSet where the values share a very long prefix path") {

        it("should create the canonical representation as soon as we just have two values left") {
            val its = LongTrieSet(8192, 2048, 8192 + 2048 + 16384, 8192 + 2048) + (8192 + 16384)
            val filteredIts = its.filter(i ⇒ i == 8192 || i == 8192 + 2048 + 16384)
            filteredIts.size should be(2)
            filteredIts shouldBe an[LongTrieSet2]
        }

        it("should create the canonical representation as soon as we just have one value left in each branch ") {
            val its = LongTrieSet(0, 8, 12, 4)
            val filteredIts = its.filter(i ⇒ i == 8 || i == 12)
            filteredIts.size should be(2)
            filteredIts shouldBe an[LongTrieSet2]
        }

        it("should create the canonical representation as soon as we just have one value left") {
            val its = LongTrieSet(8192, 2048, 8192 + 2048 + 16384, 8192 + 2048) + (8192 + 16384)
            val filteredIts = its.filter(i ⇒ i == 8192 + 2048 + 16384)
            filteredIts.size should be(1)
            filteredIts shouldBe an[LongTrieSet1]
        }
    }

    describe("an identity mapping of a small LongTrieSet") {
        it("should results in the same set") {
            val is0 = LongTrieSet.empty
            val is1 = LongTrieSet(1)
            val is2 = LongTrieSet(3, 4)
            val is3 = LongTrieSet(256, 512, 1037)

            assert(is0.map(i ⇒ i) eq is0)
            assert(is1.map(i ⇒ i) eq is1)
            assert(is2.map(i ⇒ i) eq is2)
            assert(is3.map(i ⇒ i) eq is3)
        }
    }

    describe("creation of a set via an iterator") {
        it("should result in sets comparable using subset of") {
            val s1 = IntArraySet(-147701, -141111, -7075) + 24133
            val its1 = s1.foldLeft(LongTrieSet.empty)(_ +! _.toLong)
            val its2 = s1.foldLeft(LongTrieSet.empty)(_ +! _.toLong) +! 0
            assert(its1.subsetOf(its2))
            val its1ItSet = its1.iterator.toSet
            val its2ItSet = its2.iterator.toSet
            assert(its1ItSet.subsetOf(its2ItSet))
        }
    }
}
