/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection

import org.scalacheck.Properties
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.propBoolean

import org.opalj.collection.immutable.RefArray

/**
 * Tests RefIterator (and to some degree implicitly also RefArray.)
 *
 * @author Michael Eichberg
 */
object RefIteratorProperties extends Properties("RefIterator") {

    implicit val arbRefArray: Arbitrary[RefArray[String]] = Arbitrary {
        val r = new java.util.Random()
        Gen.sized { l ⇒
            val a = RefArray._UNSAFE_from[String](new Array[AnyRef](l))
            for { i ← 0 until l } {
                a._UNSAFE_replaced(i, (r.nextInt(100) - 50).toString)
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

    property("foreach") = forAll { a: RefArray[String] ⇒
        val b = RefArray.newBuilder[String]
        val it = a.iterator
        it.foreach(b += _)
        !it.hasNext :| "consumed" && a == b.result()
    }

    property("++ (GenTraversableOnce)") = forAll { (a1: RefArray[String], l2: List[String]) ⇒
        val a = a1 ++ l2
        val it = a1.iterator ++ l2.iterator

        a.sameElements(it.toArray[String]) &&
            !it.hasNext
    }

    property("++ (RefIterator)") = forAll { (a1: RefArray[String], a2: RefArray[String]) ⇒
        classify(a1.size + a2.size == 0, "both arrays are empty") {
            val a = a1 ++ a2
            val it = a1.iterator ++ a2.iterator
            it.isInstanceOf[RefIterator[String]] &&
                a.sameElements(it.toArray[String]) &&
                !it.hasNext
        }
    }

    property("sum") = forAll { a: RefArray[String] ⇒
        classify(a.size == 0, "the array is empty") {
            val itSum = a.iterator.sum(_.toInt)
            val aSum = a.toList.map(_.toInt).sum
            itSum == aSum
        }
    }

    property("take") = forAll { (a: RefArray[String], n: Int) ⇒
        classify(a.size == 0, "the array is empty") {
            classify(n > a.size, "the number of taken elements is larger than the array") {
                val itSum = a.iterator.take(n).sum(_.toInt)
                val aSum = a.toList.take(n).map(_.toInt).sum
                itSum == aSum
            }
        }
    }

    property("drop") = forAll(listAndIntGen) { li ⇒
        val (l, i) = li
        val it = (RefArray.newBuilder[String] ++= l).result().iterator
        it.drop(i).sameElements(l.iterator.drop(i)) &&
            !it.hasNext
    }

    property("filter") = forAll { a: RefArray[String] ⇒
        val aFiltered = a.filter(_.length > 2)
        val itFiltered = a.iterator.filter(_.length > 2)
        val b = RefArray.newBuilder[String]
        itFiltered foreach (b += _)
        !itFiltered.hasNext :| "consumed" &&
            itFiltered.isInstanceOf[RefIterator[String]] &&
            aFiltered == b.result()
    }

    property("filterNot") = forAll { a: RefArray[String] ⇒
        val aFiltered = a.filterNot(_.length > 2)
        val itFiltered = a.iterator.filterNot(_.length > 2)
        val b = RefArray.newBuilder[String]
        itFiltered foreach (b += _)
        !itFiltered.hasNext :| "consumed" &&
            itFiltered.isInstanceOf[RefIterator[String]] &&
            aFiltered == b.result()
    }

    property("flatMap (GenTraversableOnce)") = forAll { a: RefArray[String] ⇒
        val itMapped: Iterator[Char] = a.iterator.flatMap(_.iterator)
        val aMapped: Seq[Char] = a.flatMap(_.iterator)
        val itAMapped = aMapped.iterator
        !itAMapped.isInstanceOf[RefIterator[_]] &&
            itAMapped.sameElements(itMapped)
    }

    property("foldLeft (AnyRef)") = forAll { a: RefArray[String] ⇒
        val itUnion = a.iterator.foldLeft("")(_ + _)
        val aUnion = a.foldLeft("")(_ + _)
        itUnion == aUnion
    }

    property("foldLeft (Int)") = forAll { a: RefArray[String] ⇒
        val itUnion = a.iterator.foldLeft(0)(_ + _.length)
        val aUnion = a.foldLeft(0)(_ + _.length)
        itUnion == aUnion
    }

    property("foldLeft (Long)") = forAll { a: RefArray[String] ⇒
        val itUnion = a.iterator.foldLeft(0L)(_ + _.length.toLong)
        val aUnion = a.foldLeft(0L)(_ + _.length.toLong)
        itUnion == aUnion
    }

    property("map (AnyRef)") = forAll { a: RefArray[String] ⇒
        val itMapped = a.iterator.map(_.drop(2))
        itMapped.isInstanceOf[RefIterator[String]] &&
            a.forall(v ⇒ v.drop(2) == itMapped.next())
    }

    property("map (Int)") = forAll { a: RefArray[String] ⇒
        val itMapped = a.iterator.map(_.length)
        itMapped.isInstanceOf[IntIterator] &&
            a.forall(v ⇒ v.length == itMapped.next())
    }

    property("map (Long)") = forAll { a: RefArray[String] ⇒
        val itMapped = a.iterator.map(_.length.toLong)
        itMapped.isInstanceOf[LongIterator] &&
            a.forall(v ⇒ v.length.toLong == itMapped.next())
    }

    property("withFilter") = forAll { a: RefArray[String] ⇒
        val aFiltered = a.withFilter(_.length > 2)
        val aBuilder = RefArray.newBuilder[String]
        aFiltered foreach (aBuilder.+=)
        val itFiltered = a.iterator.withFilter(_.length > 2)
        val itBuilder = RefArray.newBuilder[String]
        itFiltered foreach (itBuilder.+=)
        !itFiltered.hasNext :| "consumed" &&
            itFiltered.isInstanceOf[RefIterator[String]] &&
            aBuilder.result() == itBuilder.result()
    }

    property("zip") = forAll { (a1: RefArray[String], a2: RefArray[String]) ⇒
        val a = a1.zip(a2)
        val it = a1.iterator.zip(a2.iterator)

        a.sameElements(it.toArray[(String, String)]) &&
            !it.hasNext
    }

    property("zipWithIndex") = forAll { a: RefArray[String] ⇒
        val aZipped = a.zipWithIndex
        val itZipped = a.iterator.zipWithIndex

        aZipped.sameElements(itZipped.toArray[(String, Int)]) &&
            !itZipped.hasNext
    }

    property("zipWithIndex in for comprehension") = forAll { a: RefArray[String] ⇒
        val aZipped = for { (s, i) ← a.zipWithIndex } yield { s+" "+i }
        val itZipped = for { (s, i) ← a.iterator.zipWithIndex } yield { s+" "+i }
        itZipped.isInstanceOf[RefIterator[String]] &&
            (a.isEmpty && itZipped.isEmpty) || (a.nonEmpty && itZipped.nonEmpty) &&
            aZipped.sameElements(itZipped.toArray[String]) &&
            !itZipped.hasNext
    }
}
