/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package fpcf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterEach
import scala.collection.JavaConverters._
import org.opalj.log.GlobalLogContext
import org.scalatest.FunSpec

/**
 * Tests the propery store.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyStoreTest extends FunSpec with Matchers with BeforeAndAfterEach {

    var ps: PropertyStore = _

    val entities: List[String] = List(
        "a", "b", "c",
        "aa", "bb", "cc",
        "ab", "bc", "cd",
        "aaa",
        "aea",
        "aabbcbbaa",
        "aaaffffffaaa", "aaaffffffffffffffffaaa"
    )

    final val PalindromeKey = {
        PropertyKey.create[PalindromeProperty]("Palindrome", (e: Entity) ⇒ ???, (epks: Iterable[SomeEPK]) ⇒ ???)
    }
    sealed trait PalindromeProperty extends Property {
        type Self = PalindromeProperty
        def key = PalindromeKey
        def isRefineable = false
    }
    case object Palindrome extends PalindromeProperty
    case object NoPalindrome extends PalindromeProperty

    final val StringLengthKey: PropertyKey[StringLength] = {
        PropertyKey.create("StringLength", (e: Entity) ⇒ ???, (epks: Iterable[SomeEPK]) ⇒ ???)
    }
    case class StringLength(length: Int) extends Property {
        type Self = StringLength
        def key = StringLengthKey
        def isRefineable = false
    }

    override def beforeEach(): Unit = {
        ps = PropertyStore(entities, () ⇒ false, debug = false)(GlobalLogContext)
    }

    describe("set properties") {

        it("an onPropertyDerivation function should be called if entities are associated with the property afterwards") {
            val results = new java.util.concurrent.ConcurrentLinkedQueue[String]()

            ps.onPropertyDerivation(EvenNumberOfChars)(results.add(_))

            for (e ← entities if (e.size % 2) == 1) { ps.add(EvenNumberOfChars)(e) }

            ps.waitOnPropertyComputationCompletion(true)

            ps.entities(EvenNumberOfChars).asScala should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.asScala.toSet should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.size should be(6)
        }

        it("an onPropertyDerivation function should be called for all entities that already have a respective property") {
            val results = new java.util.concurrent.ConcurrentLinkedQueue[String]()

            for (e ← entities if (e.size % 2) == 1) { ps.add(EvenNumberOfChars)(e) }

            ps.onPropertyDerivation(EvenNumberOfChars)(results.add(_))

            ps.waitOnPropertyComputationCompletion(true)

            ps.entities(EvenNumberOfChars).asScala should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.asScala.toSet should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.size should be(6)
        }

        it("an onPropertyDerivation function should be called for all entities that already have a respective property and also for those that are associated with it afterwards") {
            val results = new java.util.concurrent.ConcurrentLinkedQueue[String]()

            for (e ← entities if e.size == 1) { ps.add(EvenNumberOfChars)(e) }
            ps.onPropertyDerivation(EvenNumberOfChars)(results.add(_))
            for (e ← entities if e.size % 2 == 1 && e.size != 1) { ps.add(EvenNumberOfChars)(e) }

            ps.waitOnPropertyComputationCompletion(true)

            ps.entities(EvenNumberOfChars).asScala should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.asScala.toSet should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.size should be(6)
        }

        it("deriving the same property multiple times should have no effect") {
            val results = new java.util.concurrent.ConcurrentLinkedQueue[String]()

            for (e ← entities if (e.size % 2) == 1) { ps.add(EvenNumberOfChars)(e) }
            for (e ← entities if (e.size % 2) == 1) { ps.add(EvenNumberOfChars)(e) }

            ps.onPropertyDerivation(EvenNumberOfChars)(results.add(_))

            ps.waitOnPropertyComputationCompletion(true)

            ps.entities(EvenNumberOfChars).asScala should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.asScala.toSet should be(Set("aabbcbbaa", "a", "b", "c", "aaa", "aea"))
            results.size should be(6)
        }
    }

    describe("per entity properties") {

        describe("computations depending on a specific property") {

            it("should be triggered for every entity that already have the respective property") {
                import scala.collection.mutable
                val results = mutable.Map.empty[Property, mutable.Set[String]]

                ps << { e: Entity ⇒
                    if (e.toString.reverse == e.toString())
                        ImmediateResult(e, Palindrome)
                    else
                        ImmediateResult(e, NoPalindrome)
                }

                ps.onPropertyChange(PalindromeKey)((e, p) ⇒ results.synchronized {
                    results.getOrElseUpdate(p, mutable.Set.empty[String]).add(e.toString())
                })

                ps.waitOnPropertyComputationCompletion(true)

                results(Palindrome) should be(Set("aabbcbbaa", "aa", "c", "aea", "aaa", "aaaffffffaaa", "aaaffffffffffffffffaaa", "cc", "a", "bb", "b"))
            }

            it("should be triggered for every entity that is associated with the respective property afterwards") {
                import scala.collection.mutable
                val results = mutable.Map.empty[Property, mutable.Set[String]]

                ps.onPropertyChange(PalindromeKey)((e, p) ⇒ results.synchronized {
                    results.getOrElseUpdate(p, mutable.Set.empty[String]).add(e.toString())
                })

                ps << { e: Entity ⇒
                    if (e.toString.reverse == e.toString())
                        ImmediateResult(e, Palindrome)
                    else
                        ImmediateResult(e, NoPalindrome)
                }

                ps.waitOnPropertyComputationCompletion(true)

                results(Palindrome) should be(Set("aabbcbbaa", "aa", "c", "aea", "aaa", "aaaffffffaaa", "aaaffffffffffffffffaaa", "cc", "a", "bb", "b"))
            }

            //         it("should be triggered whenever the property is updated") {
            //            import scala.collection.mutable
            //            val results = mutable.Map.empty[Entity, Property]
            //            
            //            ps.onPropertyChange(StringLengthKey)((e, p) ⇒ results.synchronized {
            //                results += ((e, p))
            //            })
            //
            //            ps << { e: Entity ⇒
            //                if (e.toString.reverse == e.toString())
            //                    ImmediateResult(e, Palindrome)
            //                else
            //                    ImmediateResult(e, NoPalindrome)
            //            }
            //
            //            ps.waitOnPropertyComputationCompletion(true)
            //
            //            results(Palindrome) should be(Set("aabbcbbaa", "aa", "c", "aea", "aaa", "aaaffffffaaa", "aaaffffffffffffffffaaa", "cc", "a", "bb", "b"))
            //        }

        }

        describe("properties") {
            it("every element can have an individual property instance") {
                import scala.collection.mutable

                ps << { e: Entity ⇒ ImmediateResult(e, StringLength(e.toString.length())) }

                ps.waitOnPropertyComputationCompletion(true)

                entities.foreach { e ⇒ ps(e, StringLengthKey).get.length should be(e.length()) }

            }
        }

    }

}

object EvenNumberOfChars extends SetProperty[String]

