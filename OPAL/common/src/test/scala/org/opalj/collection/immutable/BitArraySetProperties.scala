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

import java.util.Random

import scala.collection.immutable.{BitSet => SBitSet}

/**
 * Tests `BitArraySet`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object BitArraySetProperties extends Properties("BitArraySetProperties") {

    val r = new Random()

    val smallListsGen = for { m <- Gen.listOfN(8, Arbitrary.arbitrary[Int]) } yield (m)

    val frequencies = List(
        (1, Gen.choose(1, 31)),
        (1, Gen.choose(1, 63)),
        (1, Gen.choose(64, 127)),
        (1, Gen.choose(128, 128000))
    )

    // generates an IntArraySet which only contains positive values
    implicit val arbIntArraySet: Arbitrary[IntArraySet] = Arbitrary {
        Gen.sized { s =>
            Gen.frequency(frequencies: _*).map { max =>
                (0 until s).foldLeft(IntArraySet.empty) { (c, n) => c + r.nextInt(max) }
            }
        }
    }

    implicit val arbIntBitSet: Arbitrary[SBitSet] = Arbitrary {
        Gen.sized { s =>
            Gen.frequency(frequencies: _*).map { max =>
                (0 until s).foldLeft(SBitSet.empty) { (c, n) => c + r.nextInt(max) }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("<BitArraySet>.+|iterator(.forall)|contains") = forAll { s: IntArraySet =>
        val bas = s.foldLeft(BitArraySet.empty)(_ + _)
        classify(s.isEmpty, "empty set") {
            classify(s.nonEmpty && s.max < 32, "set with max value < 32") {
                classify(s.nonEmpty && s.max >= 32 && s.max < 64, "set with max value >=32 && < 64") {
                    (bas.isEmpty == s.isEmpty) :| "isEmpty" &&
                        bas.iterator.forall(s.contains) :| "all values in the bit array set were added" &&
                        s.iterator.forall(bas.contains) :| "the bit array set contains all values"
                }
            }
        }
    }

    property("BitArraySet.apply(value)") = forAll { s: IntArraySet =>
        s.nonEmpty ==> {
            val sIt = s.iterator
            val firstI = sIt.next()
            classify(firstI < 32, "first value <= 31") {
                classify(firstI >= 32 && firstI < 64, "31 < first value < 64") {
                    val bas = sIt.foldLeft(BitArraySet(firstI))(_ + _)
                    bas.iterator.forall(s.contains) :| "all values in the bit array set were added" &&
                        s.iterator.forall(bas.contains) :| "the bit array set contains all values"
                }
            }
        }
    }

    property("the same set is returned if a value in the set should be added") = forAll { s: IntArraySet =>
        val bas = s.foldLeft(BitArraySet.empty)(_ + _)
        s.forall(v => (bas + v) eq bas)
    }

    property("<BitArraySet>.++(|)") = forAll { (s1: SBitSet, s2: SBitSet) =>
        val bas1 = s1.foldLeft(BitArraySet.empty)(_ + _)
        val bas2 = s2.foldLeft(BitArraySet.empty)(_ + _)
        val bas3 = bas1 | bas2
        val s3 = s1 | s2

        classify(s1.isEmpty && s2.isEmpty, "both sets are empty") {
            classify(s1.nonEmpty && s2.nonEmpty && s1.max < 64 && s2.max < 64, "both sets max value < 64") {
                classify(s1.size <= s2.size, "|s1| <= |s2|", "|s1| > |s2|") {
                    bas3.iterator.forall(s3.contains) :| "all values in the bit array set were added" &&
                        s3.iterator.forall(bas3.contains) :| "the bit array set does not contain all values"
                }
            }
        }
    }

    property("adding a subset of this set to this set should return this set") = forAll { s: IntArraySet =>
        val bas1 = s.foldLeft(BitArraySet.empty)(_ + _)
        val bas2 = s.foldLeft(BitArraySet.empty)(_ + _)
        (bas1 ++ bas2) eq bas1
    }

    property("adding this set to another set of which this set is a subset of should return the other set") = forAll { (s1: IntArraySet, s2: IntArraySet) =>
        val sMerged = s1 ++ s2
        val bas1 = s1.foldLeft(BitArraySet.empty)(_ + _)
        val bas2 = s2.foldLeft(BitArraySet.empty)(_ + _)
        val basMerged = bas1 ++ bas2
        val basRemerged = (bas1 ++ basMerged /*use that...*/ )
        basMerged.iterator.forall(sMerged.contains) :| "all values in the bit array set were added" &&
            sMerged.iterator.forall(basMerged.contains) :| "the bit array set does not contain all values" &&
            (basRemerged eq basMerged) :| "same instance"
    }

    property("iterator") = forAll { s: IntArraySet =>
        val bas = s.foldLeft(BitArraySet.empty)(_ + _)
        val basIt = bas.iterator
        val basIntIt = bas.iterator
        basIt.forall(v => basIntIt.next() == v) :| "the scala iterator iterates over the same values" &&
            basIntIt.isEmpty :| "the scala iterator does not miss any values"
    }

    property("- (in order)") = forAll { (s: IntArraySet, other: IntArraySet) =>
        var bas = s.foldLeft(BitArraySet.empty)(_ + _)
        other.forall({ v => bas -= v; !bas.contains(v) }) :| "when we delete a value it is no longer in the set" &&
            s.forall({ v => bas -= v; !bas.contains(v) }) :| "we successively delete the initial values" &&
            (bas == BitArraySet0) :| "the empty set is always represented by the singleton instance"
    }

    property("- (in reverse order)") = forAll { (s: IntArraySet, other: IntArraySet) =>
        var bas = s.foldLeft(BitArraySet.empty)(_ + _)
        other.forall({ v => bas -= v; !bas.contains(v) }) :| "when we delete a value it is no longer in the set" &&
            s.toList.reverse.forall({ v =>
                bas -= v
                !bas.contains(v)
            }) :| "we successively delete the initial values in reverse order" &&
            (bas == BitArraySet0) :| "the empty set is always represented by the singleton instance"
    }

    property("equals And hashCode (expected equal)") = forAll { s: IntArraySet =>
        val bas1 = s.foldLeft(BitArraySet.empty)(_ + _)
        val bas2 = s.foldLeft(BitArraySet.empty)(_ + _)
        bas1 == bas2 && bas1.hashCode == bas2.hashCode
    }

    property("equals And hashCode") = forAll { (s1: IntArraySet, s2: IntArraySet) =>
        val bas1 = s1.foldLeft(BitArraySet.empty)(_ + _)
        val bas2 = s2.foldLeft(BitArraySet.empty)(_ + _)
        val sEquals = s1 == s2
        (bas1 == bas2) == sEquals && (!sEquals || bas1.hashCode == bas2.hashCode)
    }

    property("toString") = forAll { s: IntArraySet =>
        val bas = s.foldLeft(BitArraySet.empty)(_ + _)
        val basToString = bas.toString.substring(8)
        val sToString = s.toString.substring(8)
        val comparison = basToString == sToString
        if (!comparison) println(s""""$basToString" vs. "$sToString"""")
        comparison
    }
}
