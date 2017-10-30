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
package mutable

import org.scalacheck.Properties
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators

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
        Gen.sized { s ⇒
            Gen.frequency(frequencies: _*).map { max ⇒
                (
                    (0 until s).foldLeft(IntArraySet.empty) { (c, n) ⇒
                        c + r.nextInt(max)
                    },
                    max
                )
            }
        }
    }

    implicit val arbIntArrayListWithMax: Arbitrary[(List[Int], Int)] = Arbitrary {
        val r = new java.util.Random()
        Gen.sized { s ⇒
            Gen.frequency(frequencies: _*).map { max ⇒
                (
                    (0 until s * 2).foldLeft(List.empty[Int]) { (c, n) ⇒
                        r.nextInt(max) :: c
                    },
                    max
                )
            }
        }
    }

    property("+= (subsequently: intIterator and contains)") = forAll { (e: (IntArraySet, Int)) ⇒
        val (ias, max) = e
        //classify(ias.size == 0, "empty", s"#entries=${ias.size}; max=$max") {
        val bs = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs.intIterator.forall(ias.contains) && ias.forall(bs.contains)
        //}
    }

    property("-=") = forAll { (e1: (IntArraySet, Int)) ⇒
        val (initialIas, max) = e1
        initialIas.nonEmpty ==> {
            val bs = initialIas.foldLeft(FixedSizeBitSet.create(max))(_ += _)
            if (bs.mkString("", ",", "") != initialIas.mkString("", ",", ""))
                throw new UnknownError("initialization failed")

            var ias = initialIas
            val as = initialIas.intIterator.toArray
            (0 until as.length).forall { i ⇒
                val v = as(r.nextInt(as.length))
                ias -= v
                bs -= v
                bs.intIterator.forall(ias.contains) && ias.forall(bs.contains)
            } :| s"$ias vs $bs"
        }
    }

    property("equals and hashCode") = forAll { (e: (IntArraySet, Int)) ⇒
        val (ias, max) = e
        val bs1 = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        val bs2 = ias.toChain.reverse.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs1.hashCode == bs2.hashCode && bs1 == bs2
    }

    property("mkString") = forAll { (e: (IntArraySet, Int), x: String, y: String, z: String) ⇒
        val (ias, max) = e
        val bs1 = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        bs1.mkString(x, y, z) == ias.mkString(x, y, z)
    }

    property("iterator") = forAll { (e: (IntArraySet, Int)) ⇒
        val (ias, max) = e
        val bs1 = ias.foldLeft(FixedSizeBitSet.create(max))(_ += _)
        val bs1It = bs1.iterator
        ias.iterator.forall { i ⇒ bs1It.hasNext && bs1It.next() == i } &&
            !bs1It.hasNext
    }
}
