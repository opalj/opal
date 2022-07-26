/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.propBoolean
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

import org.opalj.util.PerformanceEvaluation

/**
 * Tests `IntTrieSet` by creating a standard Scala Set and comparing
 * the results of the respective functions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object IntTrieSetProperties extends Properties("IntTrieSet") {

    val r = new java.util.Random()

    val smallListsGen = for { m <- Gen.listOfN(8, Arbitrary.arbitrary[Int]) } yield (m)

    implicit val arbIntArraySet: Arbitrary[IntArraySet] = Arbitrary {
        Gen.sized { l =>
            val s = (0 until l).foldLeft(IntArraySet.empty) { (c, n) =>
                val nextValue = r.nextInt(75000) - 25000
                c + (if (n % 2 == 0) nextValue else -nextValue - 100000)
            }
            s
        }
    }

    implicit val arbListOfIntArraySet: Arbitrary[List[IntArraySet]] = Arbitrary {
        Gen.sized { s => Gen.listOfN(s, Arbitrary.arbitrary[IntArraySet]) }
    }

    // NOT TO BE USED BY TESTS THAT TEST THE CORRECT CONSTRUCTION!
    implicit val arbIntTrieSet: Arbitrary[IntTrieSet] = Arbitrary {
        Gen.sized { l =>
            (0 until l).foldLeft(IntTrieSet.empty) { (c, n) =>
                val nextValue = r.nextInt(75000) - 25000
                c + (if (n % 2 == 0) nextValue else -nextValue - 100000)
            }
        }
    }

    implicit val arbPairOfIntArraySet: Arbitrary[(IntArraySet, IntArraySet)] = Arbitrary {
        Gen.sized { l =>
            Gen.sized { j =>
                (
                    (0 until l).foldLeft(IntArraySet.empty) { (c, n) => c + r.nextInt(100) - 50 },
                    (0 until j).foldLeft(IntArraySet.empty) { (c, n) => c + r.nextInt(100) - 50 }
                )
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create singleton IntTrieSet") = forAll { v: Int =>
        val factoryITS = IntTrieSet1(v)
        val viaEmptyITS = EmptyIntTrieSet + v
        factoryITS.size == 1 &&
            factoryITS.isSingletonSet &&
            !factoryITS.isEmpty &&
            !factoryITS.hasMultipleElements &&
            factoryITS.head == v &&
            viaEmptyITS.hashCode == factoryITS.hashCode &&
            viaEmptyITS == factoryITS
    }

    property("create IntTrieSet from Set step by step") = forAll { s: IntArraySet =>
        val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        (its.size == s.size) :| "matching size" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.hasMultipleElements == (s.size > 1)) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toList.sorted == s.iterator.toList.sorted) :| "same content"
    }

    property("create IntTrieSet from IntSet using ++") = forAll { s: IntTrieSet =>
        val its = EmptyIntTrieSet ++ s
        (its.size == s.size) :| "matching size" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.hasMultipleElements == (s.size > 1)) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toList.sorted == s.iterator.toList.sorted) :| "same content"
    }

    property("create IntTrieSet from Set[Int] using ++") = forAll { s: Set[Int] =>
        val its = EmptyIntTrieSet ++ s
        (its.size == s.size) :| "matching size" &&
            (its.isEmpty == s.isEmpty) &&
            (its.nonEmpty == s.nonEmpty) &&
            (its.hasMultipleElements == (s.size > 1)) &&
            (its.isSingletonSet == (s.size == 1)) &&
            (its.iterator.toList.sorted == s.iterator.toList.sorted) :| "same content"
    }

    property("create IntTrieSet from List (i.e., with duplicates)") = forAll { l: List[Int] =>
        val its = l.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        val lWithoutDuplicates = l.toSet.toList
        (its.size == lWithoutDuplicates.size) :| "matching size" &&
            its.iterator.toList.sorted == lWithoutDuplicates.sorted
    }

    property("create IntTrieSet from Iterator (i.e., with duplicates)") = forAll { l: List[Int] =>
        val its = EmptyIntTrieSet ++ l.iterator
        val newits = its.iterator.toSet
        its.size == newits.size &&
            its == newits &&
            its.forall(newits.contains) &&
            newits.forall(its.contains)
    }

    property("head") = forAll { s: IntArraySet =>
        s.nonEmpty ==> {
            val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
            s.contains(its.head)
        }
    }

    property("head and headAndTail should return the same head") = forAll { s: IntTrieSet =>
        var its = s
        var success = true
        while (its.nonEmpty && success) {
            val h = its.head
            val ht = its.headAndTail
            its = ht.rest
            success = h == ht.head
        }
        success
    }

    property("mkString(String,String,String)") = forAll { (s: Set[Int], pre: String, in: String, post: String) =>
        val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        val itsString = its.mkString(pre, in, post)
        val sString = its.iterator.mkString(pre, in, post)
        val lString = s.mkString(pre, in, post)
        ((itsString.length == sString.length) :| "length of generated string (its vs s)") &&
            ((itsString.length == lString.length) :| "length of generated string (its vs l)") &&
            itsString.startsWith(pre) && itsString.endsWith(post)
    }

    property("mkString(String)") = forAll { (s: Set[Int], in: String) =>
        val its = s.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        val itsString = its.mkString(in)
        val sString = its.iterator.mkString(in)
        val lString = s.mkString(in)
        ((itsString.length == sString.length) :| "length of generated string (its vs s)") &&
            ((itsString.length == lString.length) :| "length of generated string (its vs l)")
    }

    property("contains") = forAll { (s1: IntArraySet, s2: IntArraySet) =>
        val its = s1.foldLeft(EmptyIntTrieSet: IntTrieSet)(_ + _)
        s1.forall(its.contains) :| "contains expected value" &&
            s2.forall(v => s1.contains(v) == its.contains(v))
    }

    property("foreach") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        var newS = IntArraySet.empty
        its foreach { newS += _ } // use foreach to compute a new set
        s == newS
    }

    property("foreachPair") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        var itsPairs = Set.empty[(Int, Int)]
        its foreachPair { (p1: Int, p2: Int) =>
            if (p1 < p2)
                itsPairs += ((p1, p2))
            else
                itsPairs += ((p2, p1))
        }
        var sPairs = Set.empty[(Int, Int)]
        s foreachPair { (p1: Int, p2: Int) => sPairs += ((p1, p2)) }
        (sPairs == itsPairs) :| s"$sPairs vs. $itsPairs"
    }

    property("map") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        val mappedIts = its.map(_ * 2)
        val mappedS = s.map(_ * 2)
        classify(mappedIts.size > 3, "using trie") {
            mappedS.size == mappedIts.size &&
                (EmptyIntTrieSet ++ mappedS.iterator) == mappedIts
        }
    }

    property("transform") = forAll { s: IntTrieSet =>
        val its = s.transform(_ * 2, List.newBuilder[Int]).iterator.toList.sorted
        its == s.map(_ * 2).iterator.toList.sorted
    }

    property("exists") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        classify(its.isEmpty, "the set is empty") {
            s.forall(v => its.exists(_ == v)) &&
                s.forall(v => its.exists(_ != v) == s.exists(_ != v))
        }
    }

    property("forall") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        its.forall(s.contains) &&
            its.forall(v => s.contains(-v)) == s.forall(v => s.contains(-v))
    }

    property("foldLeft") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        its.foldLeft(0)(_ + _) == s.foldLeft(0)(_ + _)
    }

    property("toChain") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        its.toList.iterator.toList.sorted == s.iterator.toList.sorted
    }

    property("headAndTail") = forAll { s: IntArraySet =>
        var its = EmptyIntTrieSet ++ s.iterator
        var removed = List.empty[Int]
        while (its.nonEmpty) {
            val IntRefPair(v, newIts) = its.headAndTail
            removed ::= v
            its = newIts
        }
        (removed.iterator.toSet.size == s.size) :| "no value is returned more than once" &&
            (removed.size == s.size) :| "all values are returned"
    }

    property("flatMap") = forAll { listOfSets: List[IntArraySet] =>
        listOfSets.nonEmpty ==> {
            val arrayOfSets = listOfSets.toArray
            val arrayOfITSets = arrayOfSets.map(s => IntTrieSet.empty ++ s.iterator)
            val l = arrayOfSets.length

            val flatMappedITSet = arrayOfITSets(0).flatMap(v =>
                arrayOfITSets(Math.abs(v) % l))
            val flatMappedSSet = arrayOfSets(0).flatMap(v =>
                arrayOfSets(Math.abs(v) % l))

            classify(flatMappedSSet.size > 50, "set with more than 50 elements") {
                classify(flatMappedSSet.size < listOfSets.map(_.size).sum, "flat map is not the join of all sets") {
                    flatMappedSSet.forall(flatMappedITSet.contains) &&
                        flatMappedSSet.size == flatMappedITSet.size
                }
            }
        }
    }

    property("not equals") = forAll { s: IntArraySet =>
        val its = EmptyIntTrieSet ++ s.iterator
        its != (new Object)
    }

    property("+!") = forAll { s: IntTrieSet =>
        val its = EmptyIntTrieSet ++! s
        its == s
    }

    property("equals") = forAll { s: IntTrieSet =>
        val i = { var i = 0; while (s.contains(i)) i += 1; i }
        val newS = s + i - i
        s == newS && s.hashCode == newS.hashCode
    }

    property("toString") = forAll { s: IntArraySet =>
        val its = s.foldLeft(IntTrieSet.empty)(_ +! _)
        val itsToString = its.toString
        itsToString.startsWith("IntTrieSet(") && itsToString.endsWith(")")
        // IMPROVE add content based test
    }

    property("subsetOf (similar)") = forAll { (s1: IntArraySet, i: Int) =>
        val its1 = s1.foldLeft(IntTrieSet.empty)(_ +! _)
        val its2 = s1.foldLeft(IntTrieSet.empty)(_ +! _) +! i
        classify(its1.size == 0, "its1 is empty") {
            classify(its1.size == 1, "its1.size == 1") {
                classify(its1.size == 2, "its1.size == 2") {
                    classify(its1.size == its2.size, "its1 == its2") {
                        its1.subsetOf(its2) == its1.iterator.toSet.subsetOf(its2.iterator.toSet)
                    }
                }
            }
        }
    }

    property("subsetOf (always succeeding)") = forAll { (s1: IntArraySet, s2: IntArraySet) =>
        val its1 = s1.foldLeft(IntTrieSet.empty)(_ + _)
        val its2 = s2.foldLeft(IntTrieSet.empty)(_ + _)
        val mergedIts = its1 ++ its2
        classify(its1.size == mergedIts.size, "its1 and its1 merged with its2 are different") {
            its1.subsetOf(mergedIts) && its2.subsetOf(mergedIts)
        }
    }

    property("filter (identity if no value is filtered)") = forAll { s: IntTrieSet =>
        val i = { var i = 0; while (s.contains(i)) i += 1; i }
        s.filter(_ != i) eq s
    }

    property("filter") = forAll { (s1: IntArraySet, s2: IntArraySet) =>
        val its1 = s1.foldLeft(IntTrieSet.empty)(_ + _)
        val its2 = s2.foldLeft(IntTrieSet.empty)(_ + _)
        val newits = its1.filter(!its2.contains(_))
        val news = s1.withFilter(!s2.contains(_))
        classify(news.size < s1.size, "filtered something") {
            news.forall(newits.contains) && newits.forall(news.contains)
        }
    }

    property("-") = forAll { ps: (IntArraySet, IntArraySet) =>
        val (s, other) = ps
        val its = s.foldLeft(IntTrieSet.empty)(_ + _)
        val newits = other.foldLeft(its)(_ - _)
        val news = other.foldLeft(s)(_ - _)
        classify(news.size < s.size, "removed something") {
            (its.size == s.size) :| "the original set is unmodified" &&
                news.forall(newits.contains) && newits.forall(news.contains)
        }
    }

    property("intersect") = forAll { ps: (IntArraySet, IntArraySet) =>
        val (ias1, ias2) = ps
        val s1 = ias1.foldLeft(IntTrieSet.empty)(_ + _)
        val s2 = ias2.foldLeft(IntTrieSet.empty)(_ + _)
        val expected = s1.foldLeft(IntTrieSet.empty)((c, n) => if (s2.contains(n)) c + n else c)
        (s1 intersect s2) == expected &&
            (s2 intersect s1) == expected
    }

    property("- (all elements)") = forAll { s: IntArraySet =>
        val its = s.foldLeft(IntTrieSet.empty)(_ + _)
        val newits = s.foldLeft(its)(_ - _)
        newits.size == 0 && newits.isEmpty &&
            newits == EmptyIntTrieSet
    }

    property("filter (all elements)") = forAll { s: IntArraySet =>
        val its = s.foldLeft(IntTrieSet.empty)(_ + _)
        its.filter(i => false) eq EmptyIntTrieSet
    }

    property("withFilter") = forAll { ss: (IntArraySet, IntArraySet) =>
        val (s1: IntArraySet, s2: IntArraySet) = ss
        val its1 = s1.foldLeft(IntTrieSet.empty)(_ + _)
        val its2 = s2.foldLeft(IntTrieSet.empty)(_ + _)
        var evaluated = false
        val newits = its1.withFilter(i => { evaluated = true; !its2.contains(i) })
        val news = s1.withFilter(!s2.contains(_))
        !evaluated :| "not eagerly evaluated" &&
            news.forall(i => newits.exists(newi => newi == i)) :| "exists check" &&
            (news.forall(newits.contains) && newits.forall(news.contains)) :| "contains check" &&
            news.forall(i => newits.iterator.contains(i)) :| s"IntIterator.contains $news vs. $newits" &&
            newits.iterator.forall(news.contains) :| "IntIterator.forall" &&
            news.forall(i => newits.iterator.contains(i)) && newits.iterator.forall(news.contains) :| "Iterator[Int]"
    }

}

@RunWith(classOf[JUnitRunner])
class IntTrieSetTest extends AnyFunSpec with Matchers {

    describe("the subset of relation") {
        it("should correctly work for empty set related comparisons") {
            assert(IntTrieSet.empty.subsetOf(IntTrieSet.empty))
            assert((IntTrieSet(1) - 1).subsetOf(IntTrieSet.empty))
            assert((IntTrieSet(1) - 1).subsetOf(IntTrieSet(1)))
            assert((IntTrieSet(1, 2, 3) - 1 - 2 - 3).subsetOf(IntTrieSet(1)))

            assert(IntTrieSet.empty.subsetOf(IntTrieSet(1)))
            assert(IntTrieSet.empty.subsetOf(IntTrieSet(1, 2)))
            assert(IntTrieSet.empty.subsetOf(IntTrieSet(1, 2, 3)))
            assert(IntTrieSet.empty.subsetOf(IntTrieSet(1, 3, 4, 5)))

        }

        it("should correctly work for set1 related comparisons") {
            assert(IntTrieSet.empty.subsetOf(IntTrieSet1(1)))
            assert(!IntTrieSet(2).subsetOf(IntTrieSet.empty))

            assert(IntTrieSet(1).subsetOf(IntTrieSet(1)))
            assert(!IntTrieSet(2).subsetOf(IntTrieSet(3)))

            assert(IntTrieSet(1).subsetOf(IntTrieSet(1, 2)))
            assert(IntTrieSet(2).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(3).subsetOf(IntTrieSet(1, 2)))

            assert(IntTrieSet(1).subsetOf(IntTrieSet(1, 2, 3)))
            assert(IntTrieSet(2).subsetOf(IntTrieSet(1, 2, 3)))
            assert(IntTrieSet(3).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(4).subsetOf(IntTrieSet(1, 2, 3)))

            assert(IntTrieSet(1).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(2).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(3).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(5).subsetOf(IntTrieSet(1, 2, 3, 4)))
        }

        it("should correctly work for set2 related comparisons") {
            assert(!IntTrieSet(1, 2).subsetOf(IntTrieSet.empty))
            assert(!IntTrieSet(1, 2).subsetOf(IntTrieSet(1)))

            assert(IntTrieSet(1, 2).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(2, 3).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(1, 3).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(-1, 1).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(-1, 2).subsetOf(IntTrieSet(1, 2)))

            assert(IntTrieSet(1, 2).subsetOf(IntTrieSet(1, 2, 3)))
            assert(IntTrieSet(1, 3).subsetOf(IntTrieSet(1, 2, 3)))
            assert(IntTrieSet(2, 3).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(-1, 2).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(1, 4).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(2, 4).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(3, 4).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(0, 4).subsetOf(IntTrieSet(1, 2, 3)))

            assert(IntTrieSet(1, 2).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(1, 3).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(1, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(2, 3).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(2, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(3, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(-1, 2).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(1, 5).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(2, 5).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(3, 5).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(4, 5).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(0, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
        }

        it("should correctly work for set3 related comparisons") {
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet.empty))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(2)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(3)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(0)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(2, 3)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 3)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 5)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(0, 5)))

            assert(IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2, 4)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(2, 3, 4)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 3, 4)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2, 0)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(2, 3, 0)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 3, 0)))

            assert(IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(1, 2, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(1, 3, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(IntTrieSet(2, 3, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2, 4, 5)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(2, 3, 4, 5)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 3, 4, 5)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 2, 0, 5)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(2, 3, 0, 5)))
            assert(!IntTrieSet(1, 2, 3).subsetOf(IntTrieSet(1, 3, 0, 5)))
        }

        it("should correctly work for set4 related comparisons") {
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet.empty))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(3)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(3, 4)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(0, 5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 3)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 4)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3, 4)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3, 4)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3, 0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3, 0)))

            assert(IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 3, 4)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 4, 5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3, 4, 5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 3, 5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3, 4, 5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 4, 0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3, 4, 0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 3, 0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3, 4, 0)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 4, -5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3, 4, -5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 3, -5)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3, 4, -2)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 4, -2)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 3, 4, -2)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(1, 2, 3, -2)))
            assert(!IntTrieSet(1, 2, 3, 4).subsetOf(IntTrieSet(2, 3, 4, -2)))
        }

        it("should correctly work for set5+ related comparisons") {

            // the following are derived from failing tests...

            val s8a = IntTrieSet(-132967, -114794, -91882, -87887) + (-23059) + (-16654) + (-623) + 35514
            val s8b = IntTrieSet(-90376, -83774, -82124, -81207) + (-18991) + (-5490) + 11406 + 11506
            assert(!s8a.subsetOf(s8b))
            assert(!s8b.subsetOf(s8a))

            val s18 = IntTrieSet.empty ++ List(-127543, -104227, -103908, -103694, -100767, -90387, -86807, -80533, -78983, -14063, -10431, -10212, -6447, -298, 163, 9627, 19840, 38723)
            val s18_plus_m1 = s18 + (-1)
            assert(s18.subsetOf(s18_plus_m1), s"$s18 expected to be subset of $s18_plus_m1")

            val s4 = IntTrieSet(-149916, -118018, -102540, -91539)
            val s4_plus_0 = s4 + 0
            assert(s4.subsetOf(s4_plus_0), s"$s4 expected to be subset of $s4_plus_0")
        }
    }

    describe("create an IntTrieSet from four values") {

        it("should contain all values if the values are distinct") {
            assert(IntTrieSet(1, 2, 3, 4).size == 4)
            assert(IntTrieSet(256, 512, 1024, 2048).size == 4)
            assert(IntTrieSet(0, 1, 10, 1000000).size == 4)
            assert(IntTrieSet(1110, 11, 10, 1).size == 4)
        }

        it("should contain only three values if two values are equal") {
            assert(IntTrieSet(1, 2, 3, 2).size == 3)
            assert(IntTrieSet(1, 1, 3, 2).size == 3)
            assert(IntTrieSet(1, 2, 3, 3).size == 3)
            assert(IntTrieSet(1, 2, 3, 1).size == 3)
        }

        it("should contain only two values if three values are equal") {
            assert(IntTrieSet(1, 2, 2, 2).size == 2)
            assert(IntTrieSet(1, 1, 2, 1).size == 2)
            assert(IntTrieSet(1, 2, 2, 2).size == 2)
            assert(IntTrieSet(2, 2, 2, 1).size == 2)
            assert(IntTrieSet(2, 2, 1, 2).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(IntTrieSet(2, 2, 2, 2).size == 1)
        }
    }

    describe("create an IntTrieSet from three values") {

        it("should contain all values if the values are distinct") {
            assert(IntTrieSet(1, 2, 4).size == 3)
            assert(IntTrieSet(256, 1024, 2048).size == 3)
            assert(IntTrieSet(0, 1, 1000000).size == 3)
            assert(IntTrieSet(1110, 11, 1).size == 3)
        }

        it("should contain only two values if two values are equal") {
            assert(IntTrieSet(1, 2, 2).size == 2)
            assert(IntTrieSet(1, 1, 2).size == 2)
            assert(IntTrieSet(1, 2, 2).size == 2)
            assert(IntTrieSet(2, 1, 2).size == 2)
            assert(IntTrieSet(2, 2, 1).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(IntTrieSet(2, 2, 2).size == 1)
        }
    }

    describe("create an IntTrieSet from two values") {

        it("should contain all values if the values are distinct") {
            assert(IntTrieSet(1, 2).size == 2)
            assert(IntTrieSet(256, 2048).size == 2)
            assert(IntTrieSet(0, 1000000).size == 2)
            assert(IntTrieSet(1110, 11).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(IntTrieSet(2, 2).size == 1)
        }
    }

    describe("create an IntTrieSet from one value") {
        it("should contain the value") {
            assert(IntTrieSet(1).head == 1)
        }
    }

    describe("regression tests") {
        it("should implement contains correctly") {
            val s = IntTrieSet(-149916, -102540, -118018, -91539) + 0
            assert(s.contains(-149916))
            assert(s.contains(-102540))
            assert(s.contains(-118018))
            assert(s.contains(-91539))
            assert(s.contains(0))

        }
    }

    describe("processing an IntTrieSet which is leaning to the right") {

        val s = IntTrieSet(8192) + 16384 + 32768 + 65536 + 131072

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

    describe("filtering an IntTrieSet where the values share a very long prefix path") {

        it("should create the canonical representation as soon as we just have two values left") {
            val its = IntTrieSet(8192, 2048, 8192 + 2048 + 16384, 8192 + 2048) + (8192 + 16384)
            val filteredIts = its.filter(i => i == 8192 || i == 8192 + 2048 + 16384)
            filteredIts.size should be(2)
            filteredIts shouldBe an[IntTrieSet2]
        }

        it("should create the canonical representation as soon as we just have one value left in each branch ") {
            val its = IntTrieSet(0, 8, 12, 4)
            val filteredIts = its.filter(i => i == 8 || i == 12)
            filteredIts.size should be(2)
            filteredIts shouldBe an[IntTrieSet2]
        }

        it("should create the canonical representation as soon as we just have one value left") {
            val its = IntTrieSet(8192, 2048, 8192 + 2048 + 16384, 8192 + 2048) + (8192 + 16384)
            val filteredIts = its.filter(i => i == 8192 + 2048 + 16384)
            filteredIts.size should be(1)
            filteredIts shouldBe an[IntTrieSet1]
        }
    }

    describe("an identity mapping of a small IntTrieSet results in the same set") {
        val is0 = IntTrieSet.empty
        val is1 = IntTrieSet(1)
        val is2 = IntTrieSet(3, 4)
        val is3 = IntTrieSet(256, 512, 1037)

        assert(is0.map(i => i) eq is0)
        assert(is1.map(i => i) eq is1)
        assert(is2.map(i => i) eq is2)
        assert(is3.map(i => i) eq is3)
    }

    describe("map using array") {
        it("should be able to map IntTrieSets where the values are shifted (partial overlap)") {
            for (length <- 0 to 50) {
                val isb = new IntTrieSetBuilder
                val a = new Array[Int](length + 5)
                for (index <- 0 until length) {
                    a(index) = index + 5
                    isb += index
                }
                val newI = isb.result()
                val newIMapped = newI.map(a)
                assert(newIMapped.size == length, s"$newI;length=$length")
                if (length > 0) {
                    assert(
                        (IntArraySet.empty ++ newIMapped.iterator).head == 5,
                        s"$newI => $newIMapped;length=$length"
                    )
                    assert(
                        (IntArraySet.empty ++ newIMapped.iterator).last == length - 1 + 5,
                        s"$newI;length=$length"
                    )
                }

            }
        }

        it("should be able to map IntTrieSets to the same values (identity mapping)") {
            for (length <- 0 to 50) {
                val isb = new IntTrieSetBuilder
                val a = new Array[Int](length + 5)
                for (index <- 0 to length) {
                    a(index) = index
                    isb += index
                }
                val newI = isb.result()
                val newIMapped = newI.map(a)
                assert(newI == newIMapped)
                assert(newI.forall(newIMapped.contains))
            }
        }

        it("should be able to map IntTrieSets to new values ") {
            for (length <- 0 to 15) {
                val isb = new IntTrieSetBuilder
                val a = new Array[Int](length + 50)
                for (index <- 0 until length) {
                    a(index) = index + 20
                    isb += index
                }
                val newI = isb.result()
                val newIMapped = newI.map(a)
                assert(newIMapped.size == length, s"$newI;length=$length")
                if (length > 0) {
                    assert(
                        (IntArraySet.empty ++ newIMapped.iterator).head == 20,
                        s"$newI => $newIMapped;length=$length"
                    )
                    assert(
                        (IntArraySet.empty ++ newIMapped.iterator).last == length - 1 + 20,
                        s"$newI;length=$length"
                    )
                }
            }
        }
    }

    describe(s"performance") {

        it("when comparing with Set[Int]") {
            val opalS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var opalS = org.opalj.collection.immutable.IntTrieSet.empty
                    for { i <- 0 to 1000000 } {
                        val v = rngGen.nextInt()
                        opalS += v
                    }
                    opalS
                } { t => info(s"IntTrieSet took ${t.toSeconds}") }
            } { mu => info(s"IntTrieSet required $mu bytes") }

            val scalaS = PerformanceEvaluation.memory {
                PerformanceEvaluation.time {
                    val seed = 123456789L
                    val rngGen = new java.util.Random(seed)
                    var scalaS = Set.empty[Int]
                    for { i <- 0 to 1000000 } {
                        val v = rngGen.nextInt()
                        scalaS += v
                    }
                    scalaS
                } { t => info(s"Set[Int] took ${t.toSeconds}") }
            } { mu => info(s"Set[Int] required $mu bytes") }

            var opalTotal = 0L
            PerformanceEvaluation.time {
                for { v <- opalS } { opalTotal += v }
            } { t => info(s"OPAL ${t.toSeconds} for foreach") }

            var scalaTotal = 0L
            PerformanceEvaluation.time {
                for { v <- scalaS } { scalaTotal += v }
            } { t => info(s"Scala ${t.toSeconds} for foreach") }

            assert(opalTotal == scalaTotal)
        }

        it("for small sets (up to 8 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i <- 1 to 3 } rngGen.nextInt();
            val setValues = (for { i <- 1 to 1000 } yield Math.abs(rngGen.nextInt())).toArray
            val queryValues = (for { i <- 1 to 1000 } yield Math.abs(rngQuery.nextInt())).toArray

            PerformanceEvaluation.time {
                for { runs <- 0 until 10000000 } {
                    var s = org.opalj.collection.immutable.IntTrieSet.empty
                    var hits = 0
                    for { i <- 0 to rngGen.nextInt(8) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t => info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for small sets (8 to 16 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i <- 1 to 3 } rngGen.nextInt();
            val setValues = (for { i <- 1 to 1000 } yield Math.abs(rngGen.nextInt())).toArray
            val queryValues = (for { i <- 1 to 1000 } yield Math.abs(rngQuery.nextInt())).toArray

            PerformanceEvaluation.time {
                for { runs <- 0 until 10000000 } {
                    var s = org.opalj.collection.immutable.IntTrieSet.empty
                    var hits = 0
                    for { i <- 0 to 8 + rngGen.nextInt(8) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t => info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for small sets (16 to 32 elements) creation and contains check should finish in reasonable time (all values are positive)") {
            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i <- 1 to 16 } rngGen.nextInt();
            val setValues = (for { i <- 1 to 10000 } yield Math.abs(rngGen.nextInt())).toArray
            val queryValues = (for { i <- 1 to 10000 } yield Math.abs(rngQuery.nextInt())).toArray

            PerformanceEvaluation.time {
                for { runs <- 0 until 1000000 } {
                    var s = org.opalj.collection.immutable.IntTrieSet.empty
                    var hits = 0
                    for { i <- 0 to 16 + rngGen.nextInt(16) } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t => info(s"${t.toSeconds} to create 1_000_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("for sets with up to 10000 elements creation and contains check should finish in reasonable time") {
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)
            val rngQuery = new java.util.Random(seed)
            // Let's ensure that the rngGen is ahead of the query one to ensure that some additions are useless...
            for { i <- 1 to 3333 } rngGen.nextInt();
            val setValues = (for { i <- 1 to 10000 } yield rngGen.nextInt()).toArray
            val queryValues = (for { i <- 1 to 10000 } yield rngQuery.nextInt()).toArray

            var sizeOfAllSets: Int = 0
            var largestSet: Int = 0
            PerformanceEvaluation.time {
                for { runs <- 0 until 10000 } {
                    var s = org.opalj.collection.immutable.IntTrieSet.empty
                    var hits = 0
                    for { i <- 1 to runs } {
                        s += setValues(i)
                        if (s.contains(queryValues(i))) hits += 1
                    }
                    largestSet = Math.max(largestSet, s.size)
                    sizeOfAllSets += s.size
                }
            } { t => info(s"${t.toSeconds} to create 10_000 sets with $sizeOfAllSets elements (largest set: $largestSet)") }
        }

        it("operations on 2500 sets with ~10000 elements each") {
            val seed = 123456789L
            val rngGen = new java.util.Random(seed)

            val allSets = PerformanceEvaluation.memory {
                for {
                    set <- 0 until 2500
                } yield {
                    var s = org.opalj.collection.immutable.IntTrieSet.empty
                    for { i <- 0 to 10000 } {
                        s += rngGen.nextInt()
                    }
                    s
                }
            } { mu => info(s"required $mu bytes") }

            var total = 0L
            PerformanceEvaluation.time {
                for { set <- allSets; v <- set } {
                    total += v
                }
            } { t => info(s"${t.toSeconds} for foreach") }

            info(s"overall size: ${allSets.map(_.size).sum}; sum: $total")
        }
    }
}
