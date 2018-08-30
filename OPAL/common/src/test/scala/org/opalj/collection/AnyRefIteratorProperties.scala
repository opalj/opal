/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

import org.scalacheck.Properties
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators

import org.opalj.collection.immutable.AnyRefArray

/**
 * Tests AnyRefIterator (and to some degree implicitly also AnyRefArray.)
 *
 * @author Michael Eichberg
 */
object AnyRefIteratorProperties extends Properties("AnyRefIterator") {

    implicit val arbAnyRefArray: Arbitrary[AnyRefArray[String]] = Arbitrary {
        val r = new java.util.Random()
        Gen.sized { l ⇒
            val a = AnyRefArray._UNSAFE_from[String](new Array[AnyRef](l))
            for { i ← 0 until l } {
                a._UNSAFE_replace(i, (r.nextInt(100) - 50).toString)
            }
            a
        }
    }

    val listAndIntGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbString.arbitrary)
        i ← Gen.choose(0, n + 2)
    } yield (m, i)

    //
    // TESTS
    //

    property("foreach") = forAll { a: AnyRefArray[String] ⇒
        val b = AnyRefArray.newBuilder[String]
        val it = a.iterator
        it.foreach(b += _)
        !it.hasNext :| "consumed" && a == b.result()
    }

    property("++ (GenTraversableOnce)") = forAll { (a1: AnyRefArray[String], l2: List[String]) ⇒
        val a = a1 ++ l2
        val it = a1.iterator ++ l2.iterator

        a.sameElements(it.toArray[String]) &&
            !it.hasNext
    }

    property("++ (AnyRefIterator)") = forAll { (a1: AnyRefArray[String], a2: AnyRefArray[String]) ⇒
        classify(a1.size + a2.size == 0, "both arrays are empty") {
            val a = a1 ++ a2
            val it = a1.iterator ++ a2.iterator
            it.isInstanceOf[AnyRefIterator[String]] &&
                a.sameElements(it.toArray[String]) &&
                !it.hasNext
        }
    }

    property("sum") = forAll { a: AnyRefArray[String] ⇒
        classify(a.size == 0, "the array is empty") {
            val itSum = a.iterator.sum(_.toInt)
            val aSum = a.toList.map(_.toInt).sum
            itSum == aSum
        }
    }

    property("drop") = forAll(listAndIntGen) { li ⇒
        val (l, i) = li
        val it = AnyRefArray(l: _*).iterator
        it.drop(i).sameElements(l.iterator.drop(i)) &&
            !it.hasNext
    }

    property("filter") = forAll { a: AnyRefArray[String] ⇒
        val aFiltered = a.filter(_.length > 2)
        val itFiltered = a.iterator.filter(_.length > 2)
        val b = AnyRefArray.newBuilder[String]
        itFiltered foreach (b += _)
        !itFiltered.hasNext :| "consumed" &&
            itFiltered.isInstanceOf[AnyRefIterator[String]] &&
            aFiltered == b.result()
    }

    property("filterNot") = forAll { a: AnyRefArray[String] ⇒
        val aFiltered = a.filterNot(_.length > 2)
        val itFiltered = a.iterator.filterNot(_.length > 2)
        val b = AnyRefArray.newBuilder[String]
        itFiltered foreach (b += _)
        !itFiltered.hasNext :| "consumed" &&
            itFiltered.isInstanceOf[AnyRefIterator[String]] &&
            aFiltered == b.result()
    }

    property("flatMap (GenTraversableOnce)") = forAll { a: AnyRefArray[String] ⇒
        val itMapped: Iterator[Char] = a.iterator.flatMap(_.iterator)
        val aMapped: Seq[Char] = a.flatMap(_.iterator)
        val itAMapped = aMapped.iterator
        !itAMapped.isInstanceOf[AnyRefIterator[_]] &&
            itAMapped.sameElements(itMapped)
    }

    property("foldLeft (AnyRef)") = forAll { a: AnyRefArray[String] ⇒
        val itUnion = a.iterator.foldLeft("")(_ + _)
        val aUnion = a.foldLeft("")(_ + _)
        itUnion == aUnion
    }

    property("foldLeft (Int)") = forAll { a: AnyRefArray[String] ⇒
        val itUnion = a.iterator.foldLeft(0)(_ + _.length)
        val aUnion = a.foldLeft(0)(_ + _.length)
        itUnion == aUnion
    }

    property("foldLeft (Long)") = forAll { a: AnyRefArray[String] ⇒
        val itUnion = a.iterator.foldLeft(0L)(_ + _.length.toLong)
        val aUnion = a.foldLeft(0L)(_ + _.length.toLong)
        itUnion == aUnion
    }

    property("map (AnyRef)") = forAll { a: AnyRefArray[String] ⇒
        val itMapped = a.iterator.map(_.drop(2))
        itMapped.isInstanceOf[AnyRefIterator[String]] &&
            a.forall(v ⇒ v.drop(2) == itMapped.next())
    }

    property("map (Int)") = forAll { a: AnyRefArray[String] ⇒
        val itMapped = a.iterator.map(_.length)
        itMapped.isInstanceOf[IntIterator] &&
            a.forall(v ⇒ v.length == itMapped.next())
    }

    property("map (Long)") = forAll { a: AnyRefArray[String] ⇒
        val itMapped = a.iterator.map(_.length.toLong)
        itMapped.isInstanceOf[LongIterator] &&
            a.forall(v ⇒ v.length.toLong == itMapped.next())
    }

    property("withFilter") = forAll { a: AnyRefArray[String] ⇒
        val aFiltered = a.withFilter(_.length > 2)
        val aBuilder = AnyRefArray.newBuilder[String]
        aFiltered foreach (aBuilder.+=)
        val itFiltered = a.iterator.withFilter(_.length > 2)
        val itBuilder = AnyRefArray.newBuilder[String]
        itFiltered foreach (itBuilder.+=)
        !itFiltered.hasNext :| "consumed" &&
            itFiltered.isInstanceOf[AnyRefIterator[String]] &&
            aBuilder.result() == itBuilder.result()
    }

    property("zip") = forAll { (a1: AnyRefArray[String], a2: AnyRefArray[String]) ⇒
        val a = a1.zip(a2)
        val it = a1.iterator.zip(a2.iterator)

        a.sameElements(it.toArray[(String, String)]) &&
            !it.hasNext
    }

    property("zipWithIndex") = forAll { a: AnyRefArray[String] ⇒
        val aZipped = a.zipWithIndex
        val itZipped = a.iterator.zipWithIndex

        aZipped.sameElements(itZipped.toArray[(String, Int)]) &&
            !itZipped.hasNext
    }

    property("zipWithIndex in for comprehension") = forAll { a: AnyRefArray[String] ⇒
        val aZipped = for { (s, i) ← a.zipWithIndex } yield { s+" "+i }
        val itZipped = for { (s, i) ← a.iterator.zipWithIndex } yield { s+" "+i }
        itZipped.isInstanceOf[AnyRefIterator[String]] &&
            (a.isEmpty && itZipped.isEmpty) || (a.nonEmpty && itZipped.nonEmpty) &&
            aZipped.sameElements(itZipped.toArray[String]) &&
            !itZipped.hasNext
    }
}

