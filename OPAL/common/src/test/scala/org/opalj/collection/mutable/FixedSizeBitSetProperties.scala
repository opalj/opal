/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import org.scalacheck.Properties
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.propBoolean

import org.opalj.collection.immutable.IntArraySet

/**
 * Tests FixedSizeBitSet.
 *
 * @author Michael Eichberg
 */
object FixedSizeBitSetProperties extends Properties("FixedSizeBitSet") {

    val r = new java.util.Random()

    val frequencies = List(
        (1, Gen.choose(1, 63)),
        (1, Gen.choose(64, 127)),
        (1, Gen.choose(128, 128000))
    )

    // generates an IntArraySet which only contains positive values
    implicit val arbIntArraySetWithMax: Arbitrary[(IntArraySet, Int)] = Arbitrary {
        Gen.sized { s =>
            Gen.frequency(frequencies: _*).map { max =>
                ((0 until s).foldLeft(IntArraySet.empty) { (c, n) => c + r.nextInt(max) }, max)
            }
        }
    }

    implicit val arbIntArrayListWithMax: Arbitrary[(List[Int], Int)] = Arbitrary {
        val r = new java.util.Random()
        Gen.sized { s =>
            Gen.frequency(frequencies: _*).map { max =>
                ((0 until s * 2).foldLeft(List.empty[Int]) { (c, n) => r.nextInt(max) :: c }, max)
            }
        }
    }

    //
    // --------------------------------- TESTS ---------------------------------
    //

    property("+= (implicitly iterator and contains)") = forAll { (e: (IntArraySet, Int)) =>
        val (ias, max) = e
        val bs = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs.isEmpty == ias.isEmpty
        bs.iterator.forall(ias.contains) && ias.forall(bs.contains)
    }

    property("-=") = forAll { (e1: (IntArraySet, Int)) =>
        val (initialIas, max) = e1
        initialIas.nonEmpty ==> {
            val bs = initialIas.foldLeft(FixedSizeBitSet.create(max))(_ += _)
            if (bs.mkString("", ",", "") != initialIas.mkString("", ",", ""))
                throw new UnknownError("initialization failed")

            var ias = initialIas
            val as = initialIas.iterator.toArray
            (0 until as.length).forall { i =>
                val v = as(r.nextInt(as.length))
                ias -= v
                bs -= v
                ias.forall(bs.contains) && !bs.contains(v)
            }
        }
    }

    property("equals and hashCode of two identical sets") = forAll { (e: (IntArraySet, Int)) =>
        val (ias, max) = e
        val bs1 = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        val bs2 = ias.toList.reverse.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs1.hashCode == bs2.hashCode && bs1 == bs2
    }

    property("equals and hashCode with empty set") = forAll { (e: (IntArraySet, Int)) =>
        val (ias, max) = e
        val bs = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        val isEmpty = ias.isEmpty
        (isEmpty && (bs == ZeroLengthBitSet) && (ZeroLengthBitSet == bs)) ||
            (!isEmpty && (bs != ZeroLengthBitSet) && (ZeroLengthBitSet != bs))
    }

    property("equals and hashCode of two arbitrary sets") = forAll { e1: (IntArraySet, Int) =>
        val (ias1, e1max) = e1
        val (ias2, e2max) = Arbitrary.arbitrary[(IntArraySet, Int)].sample.get
        val iasAreEqual = ias1 == ias2
        val bs1 = ias1.foldLeft(FixedSizeBitSet.create(e1max))(_ += _)
        val bs2 = ias2.foldLeft(FixedSizeBitSet.create(e2max))(_ += _)
        val bsAreEqual = bs1 == bs2
        iasAreEqual == bsAreEqual && (!bsAreEqual || bs1.hashCode == bs2.hashCode)
    }

    property("equals and hashCode of different sized sets with one value") =
        forAll(Gen.choose(1, 63)) { v =>
            val bs1 = FixedSizeBitSet.create(63) += v
            val bs2 = FixedSizeBitSet.create(127) += v
            val bs3 = FixedSizeBitSet.create(20000) += v
            (bs1.hashCode == bs2.hashCode) :| "bs1 & bs2 hashCodes" &&
                (bs2.hashCode == bs3.hashCode) :| "bs2 & bs3 hashCodes" &&
                (bs1 == bs2 && bs2 == bs3 && bs1 == bs3) :| "left to right" &&
                (bs2 == bs1 && bs3 == bs2 && bs3 == bs1) :| "right to left"
        }

    property("equals and hashCode of different sized, empty sets") =
        forAll(Gen.choose(0, 63)) { v =>
            val bs0 = FixedSizeBitSet.empty
            val bs1 = FixedSizeBitSet.create(63)
            val bs2 = FixedSizeBitSet.create(127)
            val bs3 = FixedSizeBitSet.create(20000)
            (bs0.hashCode == bs1.hashCode) :| "bs0 & bs1 hashCodes" &&
                (bs1.hashCode == bs2.hashCode) :| "bs1 & bs2 hashCodes" &&
                (bs2.hashCode == bs3.hashCode) :| "bs2 & bs3 hashCodes" &&
                (bs1 == bs2 && bs2 == bs3 && bs1 == bs3) :| "left to right" &&
                (bs2 == bs1 && bs3 == bs2 && bs3 == bs1) :| "right to left"
        }

    property("equals and hashCode of different sized, larger sets") =
        forAll(Gen.choose(64, 127)) { v =>
            val bs2 = FixedSizeBitSet.create(127)
            val bs3 = FixedSizeBitSet.create(20000)
            (bs2.hashCode == bs3.hashCode) :| "bs2 & bs3 hashCodes" &&
                (bs2 == bs3) :| "left to right" &&
                (bs3 == bs2) :| "right to left"
        }

    property("equals and hashCode with something else") = forAll { e: (IntArraySet, Int) =>
        val (ias, max) = e
        val bs = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs != new Object
    }

    property("mkString") = forAll { (e: (IntArraySet, Int), x: String, y: String, z: String) =>
        val (ias, max) = e
        val bs1 = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs1.mkString(x, y, z) == ias.mkString(x, y, z)
    }

    property("iterator") = forAll { (e: (IntArraySet, Int)) =>
        val (ias, max) = e
        val bs1 = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        val bs1It = bs1.iterator
        ias.iterator.forall { i => bs1It.hasNext && bs1It.next() == i } &&
            !bs1It.hasNext
    }

    property("toString") = forAll { e: (IntArraySet, Int) =>
        val (ias, max) = e
        val bs = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        val bsToString = bs.toString.substring(12)
        val iasToString = ias.toString.substring(8)
        val comparison = bsToString == iasToString
        if (!comparison) println(s""""$bsToString" vs. "$iasToString"""")
        comparison
    }
}
