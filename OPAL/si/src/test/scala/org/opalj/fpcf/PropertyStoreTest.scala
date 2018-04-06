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
package org.opalj.fpcf

//import scala.collection.mutable
//import scala.collection.{Set => SomeSet}
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import org.junit.runner.RunWith
import org.opalj.fpcf.Palindromes.SuperPalindromeProperty
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach
import org.opalj.log.GlobalLogContext

/**
 * Tests the property store.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
abstract class PropertyStoreTest extends FunSpec with Matchers with BeforeAndAfterEach {

    implicit val logContext = GlobalLogContext

    def createPropertyStore(): PropertyStore

    describe("the property store") {

        it("directly after creation it should be empty") {
            val ps = createPropertyStore()
            ps.entities(_ ⇒ true) should be('Empty)
            ps.entities(Palindromes.Palindrome) should be('Empty)
            ps.entities(Palindromes.PalindromeKey) should be('Empty)

            ps.properties("<DOES NOT EXIST>") should be('Empty)

            ps.toString(true).length should be > (0)
        }

        it("should be possible to interrupt the computations") {
            val ps = createPropertyStore()
            val doContinue = new CountDownLatch(1)
            ps.scheduleForEntity("a") { e ⇒
                val dependee = EPK("d", Palindromes.PalindromeKey)
                ps(dependee) // we use a fake dependency...
                doContinue.await() // halt this computation...
                println("we have a result...")
                IntermediateResult(
                    "a",
                    Palindromes.MaybePalindrome,
                    Seq(dependee),
                    (eps) ⇒ { Result("a", Palindromes.Palindrome) }
                )
            }
            val t: Thread = new Thread(() ⇒ ps.waitOnPhaseCompletion())
            t.start()
            ps.isInterrupted = () ⇒ true
            ps.scheduleForEntity("d")(e ⇒ Result("d", Palindromes.Palindrome))
            doContinue.countDown() // let's continue the "first computation"
            t.join()

            // test that the continuation did not lead to further tasks
            ps("a", Palindromes.PalindromeKey) should be(EPS("a", Palindromes.MaybePalindrome, false))
            // test that the scheduled computation related to "d" was not scheduled at all!
            ps("d", Palindromes.PalindromeKey) should be(EPK("d", Palindromes.PalindromeKey))

            // let's test that – if we resume the computation – the results are as expected!
            ps.isInterrupted = () ⇒ false
            ps.waitOnPhaseCompletion()
            ps("a", Palindromes.PalindromeKey) should be(EPS("a", Palindromes.Palindrome, true))
            ps("d", Palindromes.PalindromeKey) should be(EPS("d", Palindromes.Palindrome, true))
        }

        it("should be able to perform queries w.r.t. unknown entities") {
            val ps = createPropertyStore()
            val pk = Palindromes.PalindromeKey

            ps("aba", pk) should be(EPK("aba", pk))
            ps(EPK("aa", pk)) should be(EPK("aa", pk))
        }

        // test SET

        it("should set an entity's property immediately if set is used") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            val pk = Palindromes.PalindromeKey
            val ps = createPropertyStore()

            ps.set("aba", Palindrome)
            ps("aba", pk) should be(EPS("aba", Palindrome, true))

            ps.set("abca", NoPalindrome)
            ps("abca", pk) should be(EPS("abca", NoPalindrome, true))
        }

        it("should allow setting different properties w.r.t. one entity") {
            import Palindromes.Palindrome
            import Palindromes.SuperPalindrome
            import Palindromes.NoPalindrome
            import Palindromes.NoSuperPalindrome
            val ppk = Palindromes.PalindromeKey
            val sppk = Palindromes.SuperPalindromeKey
            val ps = createPropertyStore()

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)

            ps("aba", ppk) should be(EPS("aba", Palindrome, true))
            ps("aba", sppk) should be(EPS("aba", SuperPalindrome, true))

            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)
            ps("abca", ppk) should be(EPS("abca", NoPalindrome, true))
            ps("abca", sppk) should be(EPS("abca", NoSuperPalindrome, true))
        }

        it("should be able to enumerate all explicitly set properties of a entity") {
            import Palindromes.Palindrome
            import Palindromes.SuperPalindrome
            import Palindromes.NoPalindrome
            import Palindromes.NoSuperPalindrome
            val ppk = Palindromes.PalindromeKey
            val sppk = Palindromes.SuperPalindromeKey
            val ps = createPropertyStore()

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)
            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)

            ps.entities(ppk).map(_.e).toSet should be(Set("aba", "abca"))
            ps.entities(sppk).map(_.e).toSet should be(Set("aba", "abca"))

            ps.properties("aba").toSet should be(Set(Palindrome, SuperPalindrome))
        }

        it("should not set an entity's property if it already has a property") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            val ps = createPropertyStore()

            ps.set("aba", Palindrome)
            intercept[IllegalStateException] {
                ps.set("aba", NoPalindrome)
            }
        }

        it("should contain the results (at least) for all entities for which we scheduled a computation") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome

            val ps = createPropertyStore()
            val pk = Palindromes.PalindromeKey
            val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
            ps.scheduleForEntities(es) { e ⇒
                Result(e, if (e.reverse == e) Palindrome else NoPalindrome)
            }

            ps.waitOnPhaseCompletion()

            ps.entities(pk).map(_.e).toSet should be(es)
            ps.entities(p ⇒ p == Palindrome).toSet should be(Set("aba", "cc", "d", "aaabbbaaa"))
            ps.entities(p ⇒ p == NoPalindrome).toSet should be(Set("fd", "zu"))
            ps.entities(Palindrome).toSet should be(Set("aba", "cc", "d", "aaabbbaaa"))
            ps.entities(NoPalindrome).toSet should be(Set("fd", "zu"))
            ps.entities(pk).toSet should be(Set(
                EPS("aba", Palindrome, true),
                EPS("cc", Palindrome, true),
                EPS("d", Palindrome, true),
                EPS("fd", NoPalindrome, true),
                EPS("zu", NoPalindrome, true),
                EPS("aaabbbaaa", Palindrome, true)
            ))

            es.foreach { e ⇒
                val expected = if (e.reverse == e) Palindrome else NoPalindrome
                ps.properties(e).toSet should be(Set(expected))
            }
        }

        it("should trigger a lazy property computation only lazily") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome

            val ps = createPropertyStore()
            val pk = Palindromes.PalindromeKey

            ps.scheduleLazyPropertyComputation(
                pk,
                (e: Entity) ⇒ {
                    val p =
                        if (e.toString.reverse == e.toString)
                            Palindrome
                        else
                            NoPalindrome
                    Result(e, p)
                }
            )
            ps("aba", pk) should be(EPK("aba", pk))

            ps.waitOnPhaseCompletion()

            ps("aba", pk) should be(EPS("aba", Palindrome, true))
        }

        it("should not trigger a lazy property computation multiple times") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome

            val ps = createPropertyStore()
            val pk = Palindromes.PalindromeKey

            val invocationCount = new AtomicInteger(0)
            ps.scheduleLazyPropertyComputation(
                pk,
                (e: Entity) ⇒ {
                    invocationCount.incrementAndGet()
                    val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                    Result(e, p)
                }
            )
            ps("aba", pk) should be(EPK("aba", pk))
            ps("aba", pk) // just trigger it again...
            ps("aba", pk) // just trigger it again...

            ps.waitOnPhaseCompletion()

            invocationCount.get should be(1)
        }

        it("should complete the computation of dependent lazy computations before the phase ends") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            import Palindromes.NoSuperPalindrome
            import Palindromes.SuperPalindrome
            import Palindromes.MaybeSuperPalindrome
            val ppk = Palindromes.PalindromeKey
            val sppk = Palindromes.SuperPalindromeKey
            val ps = createPropertyStore()

            val invocationCount = new AtomicInteger(0)
            ps.scheduleLazyPropertyComputation(
                ppk,
                (e: Entity) ⇒ {
                    invocationCount.incrementAndGet()
                    val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                    Result(e, p)
                }
            )
            ps.scheduleLazyPropertyComputation(
                sppk,
                (e: Entity) ⇒ {
                    invocationCount.incrementAndGet()

                    val initialsExpectedEP = EPK(e, ppk)
                    ps(e, ppk) should be(initialsExpectedEP)

                    IntermediateResult(
                        e, MaybeSuperPalindrome,
                        Seq(initialsExpectedEP),
                        (eps) ⇒ {
                            if (eps.p == Palindrome /*&& ...*/ )
                                Result(e, SuperPalindrome)
                            else
                                Result(e, NoSuperPalindrome)
                        }
                    )
                }
            )
            ps.scheduleForEntity("e") { e: String ⇒
                val initiallyExpectedEP = EPK("e", sppk)
                ps("e", sppk) should be(initiallyExpectedEP)
                IntermediateResult(
                    "e", Marker.MaybeMarked,
                    Seq(initiallyExpectedEP),
                    (eps) ⇒ {
                        if (eps.p.isInstanceOf[SuperPalindromeProperty])
                            Result(e, Marker.IsMarked)
                        else
                            Result(e, Marker.NotMarked)
                    }
                )
            }

            ps.waitOnPhaseCompletion()

            ps("e", Marker.MarkerKey) should be(EPS("e", Marker.IsMarked, true))
            ps("e", ppk) should be(EPS("e", Palindrome, true))
            ps("e", sppk) should be(EPS("e", SuperPalindrome, true))
        }
    }
}

class SequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = SequentialPropertyStore()
}

// Test fixture related to a simple marker property
object Marker {
    final val MarkerKey = {
        PropertyKey.create[MarkerProperty](
            "Marker",
            (ps: PropertyStore, e: Entity) ⇒ MaybeMarked,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???
        )
    }

    sealed trait MarkerProperty extends Property {
        type Self = MarkerProperty
        def key = MarkerKey
    }
    case object IsMarked extends MarkerProperty
    case object NotMarked extends MarkerProperty
    case object MaybeMarked extends MarkerProperty
}

// Test fixture related to Palindromes.
object Palindromes {

    final val PalindromeKey = {
        PropertyKey.create[PalindromeProperty](
            "Palindrome",
            (ps: PropertyStore, e: Entity) ⇒ MaybePalindrome,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???
        )
    }

    sealed trait PalindromeProperty extends Property {
        type Self = PalindromeProperty
        def key = PalindromeKey
    }
    case object Palindrome extends PalindromeProperty
    case object NoPalindrome extends PalindromeProperty
    case object MaybePalindrome extends PalindromeProperty

    // We consider a Palindrome a SuperPalindrom if also the first half
    // is a Palindrome. If the entities' size is odd, the middle element
    // is ignored.
    final val SuperPalindromeKey = {
        PropertyKey.create[SuperPalindromeProperty](
            "SuperPalindrome",
            (ps: PropertyStore, e: Entity) ⇒ MaybeSuperPalindrome,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???
        )
    }

    sealed trait SuperPalindromeProperty extends Property {
        type Self = SuperPalindromeProperty
        def key = SuperPalindromeKey
    }
    case object SuperPalindrome extends SuperPalindromeProperty
    case object NoSuperPalindrome extends SuperPalindromeProperty
    case object MaybeSuperPalindrome extends SuperPalindromeProperty
}

