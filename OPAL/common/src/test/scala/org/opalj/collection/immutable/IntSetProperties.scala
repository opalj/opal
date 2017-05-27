/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
import org.scalacheck.Prop.forAll //, classify,
import org.scalacheck.Prop.BooleanOperators
//import org.scalacheck.Gen
//import org.scalacheck.Arbitrary

/**
 * Tests `IntSet` by creating standard Scala Set and comparing
 * the results of the respective functions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
object IntSetProperties extends Properties("IntSet") {

    /*    /**
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

    val smallListsGen = for {
        m ← Gen.listOfN(5, Arbitrary.arbitrary[String])
    } yield (m)

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
*/

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                             P R O P E R T I E S

    property("create singleton IntSet") = forAll { s: Int ⇒
        val fl1 = IntSet(s)
        val fl2 = (new IntSetBuilder += s).result
        val fl3 = IntSet1(s)
        s == fl1.head && fl1.head == fl2.head && fl2.head == fl3.head
    }

    property("size|empty|nonEmpty|hasMultipleElements") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        s.isEmpty == fl1.isEmpty && fl1.isEmpty != fl1.nonEmpty &&
            s.size == fl1.size &&
            (s.size >= 2 && fl1.hasMultipleElements) || (s.size <= 1 && !fl1.hasMultipleElements)
    }

    property("min|max|head|last") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        s.isEmpty && fl1.isEmpty || {
            s.min == fl1.min && s.max == fl1.max && fl1.min == fl1.head && fl1.max == fl1.last
        }
    }

    property("foreach") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        var newS = Set.empty[Int]
        fl1.foreach(newS += _)
        s == newS
    }

    property("withFilter") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        var newS = Set.empty[Int]
        s.withFilter(_ >= 0).withFilter(_ <= 1000).foreach(newS += _)
        fl1.withFilter(_ >= 0).withFilter(_ <= 1000).iterator.toList == newS.toList.sorted
    }

    property("map") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        val result = fl1.map(_ * 2 / 3)
        result.iterator.toList == s.map(_ * 2 / 3).toList.sorted &&
            result.isInstanceOf[IntSet]
    }

    property("-") = forAll { (s: Set[Int], v: Int) ⇒
        val fl1 = IntSetBuilder(s).result
        (s - v).toList.sorted == (fl1 - v).iterator.toList
    }

    property("+") = forAll { (s: Set[Int], v: Int) ⇒
        val fl1 = IntSetBuilder(s).result
        (s + v).toList.sorted == (fl1 + v).iterator.toList
    }

    property("subsetOf") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val fl1 = IntSetBuilder(s1).result
        val fl2 = IntSetBuilder(s2).result
        s1.subsetOf(s2) == fl1.subsetOf(fl2)
    }

    property("contains") = forAll { (s: Set[Int], v: Int) ⇒
        val fl1 = IntSetBuilder(s).result
        (s.contains(v) == fl1.contains(v)) :| "is contained in"
    }

    property("exists") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        s.exists(_ / 3 == 0) == fl1.exists(_ / 3 == 0)
    }

    property("foldLeft") = forAll { (s: Set[Int], v: String) ⇒
        val fl1 = IntSetBuilder(s).result
        (s).toList.sorted.foldLeft(v)(_ + _) == fl1.foldLeft(v)(_ + _)
    }

    property("forall") = forAll { s: Set[Int] ⇒
        val fl1 = IntSetBuilder(s).result
        s.forall(_ >= 0) == fl1.forall(_ >= 0)
    }

    property("++") = forAll { (s1: Set[Int], s2: Set[Int]) ⇒
        val fl1 = IntSetBuilder(s1).result
        val fl2 = IntSetBuilder(s2).result
        (s1 ++ s2).toList.sorted == (fl1 ++ fl2).iterator.toList
    }

    property("mkString") = forAll { (s: Set[Int], pre: String, in: String, post: String) ⇒
        val fl1 = IntSetBuilder(s).result
        s.toList.sorted.mkString(pre, in, post) == fl1.mkString(pre, in, post)
    }

}
