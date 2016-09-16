/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package collection
package immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.{forAll, classify}
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `ChainedList` by creating standard Scala Lists and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object ChainedListSpecification extends Properties("ChainedList") {

    /**
     * Generates a list and an int value in the range [0,length of list ].
     */
    val listAndIndexGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[String])
        i ← Gen.choose(0, n)
    } yield (m, i)

    val listOfListGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[List[String]])
    } yield m

    /**
     * Generates a list and an int value in the range [0,length of list +2 ].
     */
    val listAndIntGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[String])
        i ← Gen.choose(0, n + 2)
    } yield (m, i)

    val listsOfSingleCharStringsGen = for {
        n ← Gen.choose(0, 3)
        m ← Gen.choose(0, 3)
        l1 ← Gen.listOfN(n, Gen.oneOf("a", "b", "c"))
        l2 ← Gen.listOfN(m, Gen.oneOf("a", "b", "c"))
    } yield (l1, l2)

    property("create") = forAll { s: String ⇒
        val fl = ChainedList(s)
        val l = List(s)
        fl.head == l.head
    }

    property("==|hashCode") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = ChainedList(l1)
        val fl2 = ChainedList(l2)
        (l1 == l2) == (fl1 == fl2) && (fl1 != fl2 || fl1.hashCode() == fl2.hashCode())
    }

    // METHODS DEFINED BY CHAINED LIST

    property("WithFilter") = forAll { orig: List[String] ⇒
        def test(s: String): Boolean = s.length > 0
        val cl = ChainedList(orig).withFilter(test).map[String, ChainedList[String]](s ⇒ s)
        val l = orig.withFilter(test).map[String, List[String]](s ⇒ s)
        cl == ChainedList(l)
    }

    property("hasDefiniteSize") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        fl.hasDefiniteSize
    }

    property("isTraversableAgain") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        fl.isTraversableAgain
    }

    property("seq") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        (fl.seq eq fl)
    }

    property("flatMap") = forAll(listOfListGen) { l: List[List[String]] ⇒
        val fl = ChainedList(l)
        classify(l.isEmpty, "outer list is empty") {
            classify(l.nonEmpty && l.forall(_.isEmpty), "all (at least one) inner lists are empty") {
                def t(ss: List[String]): List[Int] = ss.map(_.length)
                fl.flatMap(t) == ChainedList(l.flatMap(t))
            }
        }
    }

    property("map") = forAll { l: List[String] ⇒
        def f(s: String): Int = s.length()
        val fl = ChainedList(l)
        fl.map(f) == ChainedList(l.map(f))
    }

    property("chained lists can be used in for loop") = forAll { orig: List[String] ⇒
        // In the following we, have an implicit foreach call
        var newL = List.empty[String]
        for {
            e ← ChainedList(orig)
            if e != null
        } { newL ::= e }
        newL == orig.reverse
    }

    property("chained lists can be used in for comprehensions") = forAll(listOfListGen) { orig: List[List[String]] ⇒
        // In the following we, have an implicit withFilter, map and flatMap call!
        val cl = for {
            es ← ChainedList(orig)
            if es.length > 1
            if es.length < 10
            e ← es
            r = e.capitalize
        } yield r+":"+r.length

        val l = for {
            es ← orig
            if es.length > 1
            if es.length < 10
            e ← es
            r = e.capitalize
        } yield r+":"+r.length

        cl == ChainedList(l)
    }

    property("head") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        (l.nonEmpty && l.head == fl.head) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.head; false } catch { case _: Throwable ⇒ true } }

    }

    property("tail") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        (l.nonEmpty && ChainedList(l.tail) == fl.tail) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.tail; false } catch { case _: Throwable ⇒ true } }
    }

    property("last") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        (l.nonEmpty && l.last == fl.last) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.last; false } catch { case _: Throwable ⇒ true } }
    }

    property("(is|non)Empty") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        l.isEmpty == fl.isEmpty && l.nonEmpty == fl.nonEmpty
    }

    property("apply") = forAll(listAndIndexGen) { (listAndIndex: (List[String], Int)) ⇒
        val (l, index) = listAndIndex
        classify(index == 0, "takes first") {
            classify(index == l.length - 1, "takes last") {
                val fl = ChainedList(l)
                (index < l.length && fl(index) == l(index)) ||
                    // if the index is not valid an exception
                    { try { fl(index); false } catch { case _: Throwable ⇒ true } }
            }
        }
    }

    property("exists") = forAll { (l: List[String], c: Int) ⇒
        val fl = ChainedList(l)
        def test(s: String): Boolean = s.length == c
        l.exists(test) == fl.exists(test)
    }

    property("forall") = forAll { (l: List[String], c: Int) ⇒
        val fl = ChainedList(l)
        def test(s: String): Boolean = s.length <= c
        l.forall(test) == fl.forall(test)
    }

    property("contains") = forAll { (l: List[String], s: String) ⇒
        val fl = ChainedList(l)
        l.contains(s) == fl.contains(s)
    }

    property("find") = forAll { (l: List[Int], i: Int) ⇒
        val fl = ChainedList(l)
        l.find(_ == i) == fl.find(_ == i)
    }

    property("size") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        l.size == fl.size
    }

    property(":&:") = forAll { (l: List[String], es: List[String]) ⇒
        var fle = ChainedList(l)
        var le = l
        es.forall { e ⇒
            fle :&:= e
            le ::= e
            fle.head == le.head
        }
    }

    property("foreach") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        var lRest = l
        fl.foreach { e ⇒
            if (e != lRest.head)
                throw new UnknownError;
            else
                lRest = lRest.tail
        }
        true
    }

    property("take") = forAll(listAndIntGen) { (listAndCount: (List[String], Int)) ⇒
        val (l, count) = listAndCount
        classify(l.isEmpty, "list is empty") {
            classify(count == 0, "takes no elements") {
                classify(count == l.length, "takes all elements") {
                    classify(count > l.length, "takes too many elements") {
                        val fl = ChainedList(l)
                        (
                            count <= l.length &&
                            fl.take(count) == ChainedList(l.take(count)) && fl.size == l.size
                        ) || { try { fl.take(count); false } catch { case _: Throwable ⇒ true } }
                    }
                }
            }
        }
    }

    property("takeWhile") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = ChainedList(l)
        fl.takeWhile(filter) == ChainedList(l.takeWhile(filter))
    }

    property("filter") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = ChainedList(l)
        fl.filter(filter) == ChainedList(l.filter(filter))
    }

    property("drop") = forAll(listAndIntGen) { (listAndCount: (List[String], Int)) ⇒
        val (l, count) = listAndCount
        val fl = ChainedList(l)
        (count <= l.length && fl.drop(count) == ChainedList(l.drop(count))) ||
            { try { fl.drop(count); false } catch { case _: Throwable ⇒ true } }
    }

    property("zip(GenIterable)") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = ChainedList(l1)
        classify(l1.size == l2.size, "same length") {
            fl1.zip(l2) == ChainedList(l1.zip(l2))
        }
    }

    property("zip(ChainedList)") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = ChainedList(l1)
        val fl2 = ChainedList(l2)
        classify(l1.isEmpty, "the first list is empty") {
            classify(l2.isEmpty, "the second list is empty") {
                classify(l1.size == l2.size, "same length") {
                    fl1.zip(fl2) == ChainedList(l1.zip(l2))
                }
            }
        }
    }

    property("zipWithIndex") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        classify(l.isEmpty, "empty", "non empty") {
            fl.zipWithIndex == ChainedList(l.zipWithIndex)
        }
    }

    property("corresponds") = forAll(listsOfSingleCharStringsGen) { ls ⇒
        val (l1: List[String], l2: List[String]) = ls
        def test(s1: String, s2: String): Boolean = s1 == s2
        val fl1 = ChainedList(l1)
        val fl2 = ChainedList(l2)
        classify(fl1.isEmpty && fl2.isEmpty, "both lists are empty") {
            classify(fl1.size == fl2.size, "both lists have the same length") {
                classify(l1.corresponds(l2)(test), "both lists correspond") {
                    fl1.corresponds(fl2)(test) == l1.corresponds(l2)(test)
                }
            }
        }
    }

    property("mapConserve") = forAll { (l: List[String], c: Int) ⇒
        var alwaysTrue = true
        def transform(s: String): String = { if (s.length < c) s else { alwaysTrue = false; s + c } }
        val fl = ChainedList(l)
        classify(l.forall(s ⇒ transform(s) eq s), "all strings remain the same") {
            val mappedFL = fl.mapConserve(transform)
            (mappedFL == ChainedList(l.mapConserve(transform))) &&
                (!alwaysTrue || (fl eq mappedFL))
        }
    }

    property("reverse") = forAll { l: List[String] ⇒
        val fl = ChainedList(l)
        fl.reverse == ChainedList(l.reverse)
    }

    property("mkString") = forAll { (l: List[String], pre: String, sep: String, post: String) ⇒
        val fl = ChainedList(l)
        fl.mkString(pre, sep, post) == l.mkString(pre, sep, post)
    }

    property("toIterable") = forAll { l: List[String] ⇒
        val fl = ChainedList(l).toIterable.toList
        fl == l
    }
    property("toIterator") = forAll { l: List[String] ⇒
        val fl = ChainedList(l).toIterator.toList
        fl == l
    }
    property("toTraversable") = forAll { l: List[String] ⇒
        val fl = ChainedList(l).toTraversable.toList
        fl == l
    }
    property("toStream") = forAll { l: List[String] ⇒
        val fl = ChainedList(l).toStream
        fl == l.toStream
    }
    property("copyToArray") = forAll(listAndIntGen) { v ⇒
        val (l, i) = v
        val fl = ChainedList(l)
        val la = new Array[String](10)
        val fla = new Array[String](10)
        l.copyToArray(la, i / 3, i * 2)
        fl.copyToArray(fla, i / 3, i * 2)
        la.zip(fla).forall(e ⇒ e._1 == e._2)
    }

    property("toString") = forAll { l: List[String] ⇒
        ChainedList(l).toString.endsWith("ChainedNil")
    }

}

/*
 * PERFORMANCE EVALUATION
 * /// USING SCALA LIST
 *
 * var l : List[Long] = Nil
 *
 * def take2MultipleAndAdd() = {
 * var rest = l
 * if (rest.nonEmpty) {
 * val first = rest.head
 * rest = rest.tail
 * if (rest.nonEmpty) {
 * rest ::= first* rest.head
 * } else {
 * rest = l
 * }
 * }
 * rest
 * }
 *
 * def sum() : Long = {
 * var sum = 0l
 * var rest = l
 * while (rest.nonEmpty) {
 * sum += rest.head
 * rest = rest.tail
 * }
 * sum
 * }
 *
 *
 * {
 * val startTime = System.nanoTime
 * for (i <- 0 to 500) {
 * var j = 0
 * while (j < i) {
 * j += 1
 * l ::= 10
 * if (j % 20 == 0) sum()
 * else if (j % 3 == 0) take2MultipleAndAdd()
 * }
 * }
 * println("Elapsed time: "+(System.nanoTime-startTime))
 * }
 *
 * sum()
 *
 *
 * ///
 * /// USING CHAINED LIST
 * ///
 * import org.opalj.collection.immutable._
 * var l : ChainedList[Long] = ChainedNil
 *
 * def take2MultipleAndAdd() = {
 * var rest = l
 * if (rest.nonEmpty) {
 * val first = rest.head
 * rest = rest.tail
 * if (rest.nonEmpty) {
 * rest :&:= first* rest.head
 * } else {
 * rest = l
 * }
 * }
 * rest
 * }
 *
 * def sum() : Long = {
 * var sum = 0l
 * var rest = l
 * while (rest.nonEmpty) {
 * sum += rest.head
 * rest = rest.tail
 * }
 * sum
 * }
 *
 * {
 * val startTime = System.nanoTime
 * for (i <- 0 to 500) {
 * var j = 0
 * while (j < i) {
 * j += 1
 * l :&:= 10
 * if (j % 20 == 0) sum()
 * else if (j % 3 == 0) take2MultipleAndAdd()
 * }
 * }
 * println("Elapsed time: "+(System.nanoTime-startTime))
 * }
 *
 * sum()
 * 
 */
