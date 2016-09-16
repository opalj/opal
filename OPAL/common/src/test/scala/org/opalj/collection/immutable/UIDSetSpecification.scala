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
import org.scalacheck.Prop.{forAll, classify, BooleanOperators}
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

/**
 * Tests `UIDSets` by creating standard Sets and comparing
 * the results of the respective functions modulo the different semantics.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object UIDSetSpecification extends Properties("UIDSet") {

    case class SUID(id: Int) extends UID
    type SUIDSet = UIDSet[SUID]
    val EmptySUIDSet: SUIDSet = UIDSet0

    implicit def intToSUID(i: Int): SUID = SUID(i)

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // 							G E N E R A T O R S

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 							P R O P E R T I E S

    property("create singleton set") = forAll { i: Int ⇒
        val s = SUID(i)
        val fl1 = UIDSet[SUID](i)
        val fl2 = (UIDSet.newBuilder[SUID] += i).result
        val fl3 = (UIDSet0: UIDSet[SUID]) + s
        (fl1.head == s) :| "apply" && (fl2.head == s) :| "builder" && (fl3.head == s) :| "+"
    }

    property("create set incrementally") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        s.size == us.size && s.forall(us.contains(_)) && us.forall(s.contains(_))
    }

    property("create set given two values") = forAll { (a: Int, b: Int) ⇒
        val us = UIDSet(SUID(a), SUID(b))
        (a == b && us.size == 1) || (a != b && us.size == 2)
    }

    property("==|hashCode[AnyRef]") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val us1 = UIDSet.empty[SUID] ++ s1.map(SUID(_))
        val us2 = UIDSet.empty[SUID] ++ s2.map(SUID(_))
        classify(s1 == s2, "both sets are equal") {
            (s1 == s2) == (us1 == us2) && (us1 != us2 || us1.hashCode() == us2.hashCode())
        }
    }

    // METHODS DEFINED BY UIDSet

    property("isSingletonSet") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        (s.size == 1) == us.isSingletonSet
    }

    property("WithFilter") = forAll { orig: Set[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        def test(s: SUID): Boolean = s.id > 0
        s.withFilter(test).map((s: SUID) ⇒ s) == us.withFilter(test).map((s: SUID) ⇒ s)(UIDSet.canBuildSetFromUIDSet)
    }

    property("uid sets can be used in for loop") = forAll { orig: List[Int] ⇒
        val s = orig.map(SUID(_))
        val us = EmptySUIDSet ++ s
        var newUs = EmptySUIDSet
        for {
            s ← us
            if s != SUID(0)
        } {
            newUs += s
        }
        newUs == us.filter(_ != SUID(0))
    }

    /*
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

    property("a for-comprehension constructs specialized lists when possible") = forAll { orig: List[List[Int]] ⇒
        // In the following we, have an implicit withFilter, map and flatMap call!
        val cl = for { es ← ChainedList(orig) } yield ChainedList(es)
        cl.forall(icl ⇒ isSpecialized(icl))
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

    property(":&::") = forAll { (l1: List[String], l2: List[String]) ⇒
        val fl1 = ChainedList(l1)
        val fl2 = ChainedList(l2)
        val fl3 = fl2 :&:: fl1
        val l3 = l2 ::: l1
        fl3.toList == l3
    }

    property("++") = forAll { (l1: List[String], l2: List[String]) ⇒
        l1.nonEmpty ==> {
            val fl1 = ChainedList(l1)
            val fl2 = ChainedList(l2)
            val fl3 = fl1.asInstanceOf[:&:[String]] ++ fl2
            val l3 = l1 ++ l2
            fl3.toList == l3
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

    property("dropWhile") = forAll { (l: List[String], c: Int) ⇒
        def filter(s: String): Boolean = s.length() < 2
        val fl = ChainedList(l)
        fl.dropWhile(filter) == ChainedList(l.dropWhile(filter))
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
*/
}
