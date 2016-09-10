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
 * Tests `FastList` by creating standard Scala Lists and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object FastListSpecification extends Properties("FastList") {

    /**
     * Generates a list and an int value in the range [0,length of list ].
     */
    val listAndIndexGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[String])
        i ← Gen.choose(0, n)
    } yield (m, i)

    /**
     * Generates a list and an int value in the range [0,length of list +2 ].
     */
    val listAndIntGen = for {
        n ← Gen.choose(0, 20)
        m ← Gen.listOfN(n, Arbitrary.arbitrary[String])
        i ← Gen.choose(0, n + 2)
    } yield (m, i)

    property("==|hashCode") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = FastList(l1)
        val fl2 = FastList(l2)
        (l1 == l2) == (fl1 == fl2) && (fl1 != fl2 || fl1.hashCode() == fl2.hashCode())
    }

    property("head") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        (l.nonEmpty && l.head == fl.head) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.head; false } catch { case _: Throwable ⇒ true } }

    }

    property("tail") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        (l.nonEmpty && FastList(l.tail) == fl.tail) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.tail; false } catch { case _: Throwable ⇒ true } }
    }

    property("last") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        (l.nonEmpty && l.last == fl.last) ||
            // if the list is empty, an exception needs to be thrown
            { try { fl.last; false } catch { case _: Throwable ⇒ true } }
    }

    property("(is|non)Empty") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        l.isEmpty == fl.isEmpty && l.nonEmpty == fl.nonEmpty
    }

    property("apply") = forAll(listAndIndexGen) { (listAndIndex: (List[String], Int)) ⇒
        val (l, index) = listAndIndex
        classify(index == 0, "takes first") {
            classify(index == l.length - 1, "takes last") {
                val fl = FastList(l)
                (index < l.length && fl(index) == l(index)) ||
                    // if the index is not valid an exception
                    { try { fl(index); false } catch { case _: Throwable ⇒ true } }
            }
        }
    }

    property("exists") = forAll { (l: List[String], c: Int) ⇒
        val fl = FastList(l)
        def test(s: String): Boolean = s.length == c
        l.exists(test) == fl.exists(test)
    }

    property("forall") = forAll { (l: List[String], c: Int) ⇒
        val fl = FastList(l)
        def test(s: String): Boolean = s.length == c
        l.forall(test) == fl.forall(test)
    }

    property("contains") = forAll { (l: List[String], s: String) ⇒
        val fl = FastList(l)
        l.contains(s) == fl.contains(s)
    }

    property("size") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        l.size == fl.size
    }

    property(":!:") = forAll { (l: List[String], es: List[String]) ⇒
        var fle = FastList(l)
        var le = l
        es.forall { e ⇒
            fle :!:= e
            le ::= e
            fle.head == le.head
        }
    }

    property("foreach") = forAll { l: List[String] ⇒
        val fl = FastList(l)
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
                        val fl = FastList(l)
                        (count <= l.length && fl.take(count) == FastList(l.take(count)) && fl.size == l.size) ||
                            { try { fl.take(count); false } catch { case _: Throwable ⇒ true } }
                    }
                }
            }
        }
    }

    property("takeWhile") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = FastList(l)
        fl.takeWhile(filter) == FastList(l.takeWhile(filter))
    }

    property("filter") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() >= c
        val fl = FastList(l)
        fl.filter(filter) == FastList(l.filter(filter))
    }

    property("drop") = forAll(listAndIndexGen) { (listAndCount: (List[String], Int)) ⇒
        val (l, count) = listAndCount
        val fl = FastList(l)
        val fldrop = fl.drop(count)
        val dropfl = FastList(l.drop(count))
        (count <= l.length && fldrop == dropfl && fldrop.size == dropfl.size) ||
            { try { fl.take(count); false } catch { case _: Throwable ⇒ true } }
    }

    property("map") = forAll { l: List[String] ⇒
        def f(s: String): Int = s.length()
        val fl = FastList(l)
        fl.map(f) == FastList(l.map(f))
    }

    property("zip(GenIterable)") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = FastList(l1)
        classify(l1.size == l2.size, "same length") {
            fl1.zip(l2) == FastList(l1.zip(l2))
        }
    }

    property("zip(FastList)") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = FastList(l1)
        val fl2 = FastList(l2)
        classify(l1.isEmpty, "the first list is empty") {
            classify(l2.isEmpty, "the second list is empty") {
                classify(l1.size == l2.size, "same length") {
                    fl1.zip(fl2) == FastList(l1.zip(l2))
                }
            }
        }
    }

    property("zipWithIndex") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        classify(l.isEmpty, "empty", "non empty") {
            fl.zipWithIndex == FastList(l.zipWithIndex)
        }
    }

    property("corresponds") = forAll { (l1: List[String], l2: List[String]) ⇒
        def test(s1: String, s2: String): Boolean = s1.length == s2.length
        val fl1 = FastList(l1)
        val fl2 = FastList(l2)

        fl1.corresponds(fl2)(test) == l1.corresponds(l2)(test)
    }

    property("mapConserve") = forAll { (l: List[String], c: Int) ⇒
        var alwaysTrue = true
        def transform(s: String): String = { if (s.length < c) s else { alwaysTrue = false; s + c } }
        val fl = FastList(l)
        classify(l.forall(s ⇒ transform(s) eq s), "all strings remain the same") {
            val mappedFL = fl.mapConserve(transform)
            (mappedFL == FastList(l.mapConserve(transform))) &&
                (!alwaysTrue || (fl eq mappedFL))
        }
    }

    property("reverse") = forAll { l: List[String] ⇒
        val fl = FastList(l)
        fl.reverse == FastList(l.reverse)
    }

    property("mkString") = forAll { (l: List[String], pre: String, sep: String, post: String) ⇒
        val fl = FastList(l)
        fl.mkString(pre, sep, post) == l.mkString(pre, sep, post)
    }
}
