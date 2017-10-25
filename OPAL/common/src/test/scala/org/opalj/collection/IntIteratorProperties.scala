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

//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.Gen
//import org.scalacheck.util.Buildable
import org.scalacheck.Arbitrary
import org.opalj.collection.immutable.IntArraySet

/**
 * Tests IntIterator.
 *
 * @author Michael Eichberg
 */
object IntIteratorProperties extends Properties("IntIterator") {

    implicit val arbIntArraySet: Arbitrary[IntArraySet] = Arbitrary {
        val r = new java.util.Random()
        Gen.sized { l ⇒
            var is = IntArraySet.empty
            (0 until l).foreach { i ⇒
                is += r.nextInt(100) - 50
            }
            is
        }
    }

    property("exists") = forAll { (is: IntArraySet, v: Int) ⇒
        classify(is.contains(v), "v is in the set") {
            is.intIterator.exists(_ == v) == is.iterator.exists(_ == v)
        }
    }

    property("forall") = forAll { (is: IntArraySet, v: Int) ⇒
        classify(is.contains(v), "v is in the set") {
            is.intIterator.forall(_ != v) == is.iterator.forall(_ != v)
        }
    }

    property("contains") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        values.forall { v ⇒
            is.intIterator.contains(v) == is.iterator.contains(v)
        }
    }

    property("filter") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        (is.intIterator.filter(values.contains).forall(values.contains) :| "filter => forall") &&
            is.intIterator.filter(values.contains).toChain == is.withFilter(values.contains).toChain
    }

    property("withFilter") = forAll { (is: IntArraySet, values: IntArraySet) ⇒
        is.intIterator.withFilter(values.contains).toChain == is.withFilter(values.contains).toChain
    }

    property("foldLeft") = forAll { (is: IntArraySet) ⇒
        is.intIterator.foldLeft(0)(_ + _) == is.iterator.foldLeft(0)(_ + _)
    }

    property("map") = forAll { (is: IntArraySet) ⇒
        is.intIterator.map(_ + 1).toChain == is.map(_ + 1).toChain
    }

    property("toArray") = forAll { (is: IntArraySet) ⇒
        java.util.Arrays.equals(is.intIterator.toArray, is.toChain.toArray) :| is.intIterator.toArray.mkString(",")+" vs. "+is.toChain.toArray.mkString(",")
    }

    property("toChain") = forAll { (is: IntArraySet) ⇒
        is.intIterator.toChain == is.toChain
    }

    property("mkString") = forAll { (is: IntArraySet) ⇒
        is.intIterator.mkString("-", ";", "-!") == is.iterator.mkString("-", ";", "-!")
    }
}
