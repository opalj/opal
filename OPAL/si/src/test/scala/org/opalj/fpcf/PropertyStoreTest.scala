/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable
import scala.collection.mutable
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.fpcf.fixtures._

/**
 * Tests a property store implementation. All of the following tests should be passed by
 * parallel and or sequential store implementations and also independent of any optimizations.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
sealed abstract class PropertyStoreTest[PS <: PropertyStore]
    extends AnyFunSpec
    with Matchers
    with BeforeAndAfterAll {

    implicit val logContext: LogContext = GlobalLogContext

    def createPropertyStore(): PS

    /**
     * Called by the tests to perform some further general testing of the
     * property store independent of the concrete test case.
     *
     * Called before the property store is shut down by the tests.
     */
    def afterAll(ps: PS): Unit = {}

    describe(s"PropertyStore") {

        import Palindromes.NoAnalysisForPalindromeProperty
        import Palindromes.NoPalindrome
        import Palindromes.NoSuperPalindrome
        import Palindromes.Palindrome
        import Palindromes.PalindromeKey
        import Palindromes.PalindromePropertyNotAnalyzed
        import Palindromes.SuperPalindrome
        import Palindromes.SuperPalindromeKey
        import Palindromes.PalindromeFragmentsKey
        import Palindromes.PalindromeFragments
        import ReachableEntities.ReachedEntities
        import ReachableEntities.ReachedEntitiesKey

        it("should be possible to query an empty property store") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.quiescenceCount should be(0)
            ps.scheduledTasksCount should be(0)
            ps.scheduledOnUpdateComputationsCount should be(0)
            ps.isKnown("<DOES NOT EXIST>") should be(false)
            ps.hasProperty("<DOES NOT EXIST>", Palindrome) should be(false)
            ps.properties("<DOES NOT EXIST>") should be(Symbol("Empty"))
            ps.entities(_ => true) should be(Symbol("Empty"))
            ps.entities(Palindrome, Palindrome) should be(Symbol("Empty"))
            ps.entities(PalindromeKey) should be(Symbol("Empty"))
            ps.finalEntities(Palindrome) should be(Symbol("Empty"))

            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

            ps.isKnown("<DOES NOT EXIST>") should be(false)
            ps.hasProperty("<DOES NOT EXIST>", Palindrome) should be(false)
            ps.properties("<DOES NOT EXIST>") should be(Symbol("Empty"))
            ps.entities(_ => true) should be(Symbol("Empty"))
            ps.entities(Palindrome, Palindrome) should be(Symbol("Empty"))
            ps.entities(PalindromeKey) should be(Symbol("Empty"))
            ps.finalEntities(Palindrome) should be(Symbol("Empty"))

            ps.waitOnPhaseCompletion()

            ps.quiescenceCount should be(1)
            ps.scheduledOnUpdateComputationsCount should be(0)
            ps.isKnown("<DOES NOT EXIST>") should be(false)
            ps.hasProperty("<DOES NOT EXIST>", Palindrome) should be(false)
            ps.properties("<DOES NOT EXIST>") should be(Symbol("Empty"))
            ps.entities(_ => true) should be(Symbol("Empty"))
            ps.entities(Palindrome, Palindrome) should be(Symbol("Empty"))
            ps.entities(PalindromeKey) should be(Symbol("Empty"))
            ps.finalEntities(Palindrome) should be(Symbol("Empty"))

            afterAll(ps)
            ps.shutdown()
        }

        it("should be able to print properties even if the store is empty") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.toString(true).length should be > (0)
            ps.toString(false).length should be > (0)

            ps.setupPhase(Set(PalindromeKey), Set.empty)

            ps.toString(true).length should be > (0)
            ps.toString(false).length should be > (0)

            ps.waitOnPhaseCompletion()

            ps.toString(true).length should be > (0)
            ps.toString(false).length should be > (0)

            afterAll(ps)
            ps.shutdown()
        }

        it("should be possible to terminate the computations and still query the store") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(PalindromeKey), Set.empty)
            ps.scheduleEagerComputationForEntity("a") { e =>
                ps.doTerminate = true
                val dependee = ps(EPK("d", PalindromeKey)) // we use a fake dependency...
                IncrementalResult(
                    InterimResult(
                        "a",
                        NoPalindrome,
                        Palindrome,
                        Set(dependee),
                        eps => { Result("a", Palindrome) }
                    ),
                    // This computation should not be executed!
                    Iterator.single(
                        (
                            (e: Entity) => { fail("should not be executed") }: Result,
                            "d"
                        )
                    )
                )
            }
            try {
                ps.waitOnPhaseCompletion()
                fail("expected InterruptedException")
            } catch {
                case _: InterruptedException => // OK
                case t: Throwable =>
                    val m =
                        t.getStackTrace
                            .take(25)
                            .map(_.toString)
                            .mkString(
                                s"unexpected exception (${t.getClass.getSimpleName}): "+
                                    s"${t.getMessage}\n\t",
                                "\n\t",
                                ""
                            )
                    fail(m)
            }

            afterAll(ps)
            ps.shutdown()
        }

        it("should be possible to call setupPhase after waitOnPhaseCompletion") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(PalindromeKey), Set.empty)
            ps.scheduleEagerComputationForEntity("a") { e =>
                val dependee = EPK("d", Palindromes.PalindromeKey)
                ps(dependee) // we use a fake dependency...
                Result("a", NoPalindrome)
            }
            ps.waitOnPhaseCompletion()

            ps.setupPhase(Set.empty, Set.empty)

            ps("a", PalindromeKey) should be(FinalEP("a", NoPalindrome))

            afterAll(ps)
            ps.shutdown()
        }

        it("should not crash when e1 has two dependencies e2 and e3 and e2 is set "+
            "while e1 was not yet executed but had an EPK for e2 in its dependencies "+
            "(test for a lost updated)") {

            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.set("e2", Palindrome)

            ps.setupPhase(Set(PalindromeKey), Set.empty)

            ps.scheduleEagerComputationForEntity("e1") { e =>
                val e3EPK = EPK("e3", PalindromeKey)
                val dependees: Set[SomeEOptionP] = Set(EPK("e2", PalindromeKey), e3EPK)
                ps(e3EPK) // we have to query it (e2 is already set => no need to query it)!
                InterimResult(
                    "e1",
                    NoPalindrome,
                    Palindrome,
                    dependees,
                    eps => { Result("e1", Palindrome) }
                )
            }
            ps.waitOnPhaseCompletion()

            ps("e1", PalindromeKey) should be(FinalEP("e1", Palindrome))
            ps("e2", PalindromeKey) should be(FinalEP("e2", Palindrome))
            ps("e3", PalindromeKey) should be(FinalEP("e3", PalindromePropertyNotAnalyzed))

            afterAll(ps)
            ps.shutdown()
        }

        it("should be able to perform queries w.r.t. unknown entities / property keys") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(PalindromeKey), Set.empty)
            ps("aba", PalindromeKey) should be(EPK("aba", PalindromeKey))
            ps(EPK("aa", PalindromeKey)) should be(EPK("aa", PalindromeKey))
            ps.waitOnPhaseCompletion()

            afterAll(ps)
            ps.shutdown()
        }

        it("should be possible to test if a store has a property even if it was only queried") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.hasProperty("aba", PalindromeKey) should be(false)
            ps.hasProperty("aba", SuperPalindromeKey) should be(false)

            ps.set("aba", Palindrome)
            ps.set("zzYzz", SuperPalindrome)

            ps.hasProperty("aba", PalindromeKey) should be(true)
            ps.hasProperty("zzYzz", SuperPalindromeKey) should be(true)

            ps.hasProperty("aba", SuperPalindromeKey) should be(false)

            ps.setupPhase(Set(PalindromeKey, SuperPalindromeKey), Set.empty)
            ps.waitOnPhaseCompletion()

            ps.isKnown("aba") should be(true)

            ps.hasProperty("aba", PalindromeKey) should be(true)
            ps.hasProperty("zzYzz", SuperPalindromeKey) should be(true)

            ps.hasProperty("cbc", PalindromeKey) should be(false)
            ps.hasProperty("aba", SuperPalindromeKey) should be(false)

            afterAll(ps)
            ps.shutdown()
        }

        it("should be possible to test if a store has a property "+
            "even if the analysis is scheduled later") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.hasProperty("aba", PalindromeKey) should be(false)
            ps.hasProperty("aba", SuperPalindromeKey) should be(false)

            ps.set("aba", Palindrome)
            ps.set("zzYzz", SuperPalindrome)

            ps.hasProperty("aba", PalindromeKey) should be(true)
            ps.hasProperty("zzYzz", SuperPalindromeKey) should be(true)

            ps.hasProperty("aba", SuperPalindromeKey) should be(false)

            ps.setupPhase(Set(PalindromeKey, SuperPalindromeKey), Set.empty)

            ps.scheduleEagerComputationForEntity("a") { e =>
                val dependee = EPK("d", PalindromeKey)
                ps(dependee)
                InterimResult(
                    "a",
                    NoPalindrome, Palindrome,
                    Set(dependee),
                    _ => Result("a", Palindrome)
                )
            }
            ps.waitOnPhaseCompletion()

            ps.hasProperty("a", PalindromeKey) should be(true)
            ps.hasProperty("d", PalindromeKey) should be(true)

            afterAll(ps)
            ps.shutdown()
        }

        // test SET

        it("set should set an entity's property \"immediately\"") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.set("aba", Palindrome)
            ps.set("abca", NoPalindrome)

            ps.setupPhase(Set(Palindrome.key), Set.empty)
            ps.waitOnPhaseCompletion()

            ps("aba", PalindromeKey) should be(FinalEP("aba", Palindrome))
            ps("abca", PalindromeKey) should be(FinalEP("abca", NoPalindrome))

            afterAll(ps)
            ps.shutdown()
        }

        it("should allow setting different properties w.r.t. one entity") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)

            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)

            ps.setupPhase(Set(Palindrome.key), Set.empty)
            ps.waitOnPhaseCompletion()

            ps("aba", PalindromeKey) should be(FinalEP("aba", Palindrome))
            ps("aba", SuperPalindromeKey) should be(FinalEP("aba", SuperPalindrome))

            ps("abca", PalindromeKey) should be(FinalEP("abca", NoPalindrome))
            ps("abca", SuperPalindromeKey) should be(FinalEP("abca", NoSuperPalindrome))

            afterAll(ps)
            ps.shutdown()
        }

        it("should be able to enumerate all explicitly set properties of an entity") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            val ppk = PalindromeKey
            val sppk = SuperPalindromeKey
            ps.setupPhase(Set.empty, Set.empty)

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)
            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)
            ps.waitOnPhaseCompletion()
            ps.entities(ppk).map(_.e).toSet should be(Set("aba", "abca"))
            ps.entities(sppk).map(_.e).toSet should be(Set("aba", "abca"))
            val expected = Set(FinalEP("aba", Palindrome), FinalEP("aba", SuperPalindrome))
            ps.properties("aba").toSet should be(expected)

            afterAll(ps)
            ps.shutdown()
        }

        it("should specify that no analysis was scheduled when requesting the fallback in the respective case") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(Marker.Key), Set.empty)

            val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
            ps.scheduleEagerComputationsForEntities[String](es) { e =>
                def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                    (eps: @unchecked) match {
                        case FinalP(NoAnalysisForPalindromeProperty) /*<= the test...*/ =>
                            Result(e, Marker.NotMarked)
                        case epk: SomeEPK =>
                            InterimResult(e, Marker.IsMarked, Marker.NotMarked, Set(epk), c)
                    }
                }
                c(ps(e, Palindromes.PalindromeKey))
            }

            ps.waitOnPhaseCompletion()

            ps.entities(Marker.NotMarked, Marker.NotMarked).toSet should be(es)

            afterAll(ps)
            ps.shutdown()
        }

        it("should specify that the property was not derived when an analysis was scheduled") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(PalindromeKey, Marker.Key), Set.empty)

            val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
            ps.scheduleEagerComputationsForEntities(Set("aba", "cc", "d")) { e =>
                Result(e, if (e.reverse == e) Palindrome else NoPalindrome)
            }
            ps.scheduleEagerComputationsForEntities(es) { e =>
                def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                    (eps: @unchecked) match {
                        case FinalP(Palindrome) | FinalP(NoPalindrome) =>
                            Result(e, Marker.IsMarked)

                        case FinalP(PalindromePropertyNotAnalyzed) =>
                            Result(e, Marker.NotMarked)

                        case epk: SomeEPK => InterimResult(
                            e, Marker.IsMarked, Marker.NotMarked, Set(epk), c
                        )
                    }
                }
                c(ps(e, PalindromeKey))
            }

            ps.waitOnPhaseCompletion()
            ps.setupPhase(Set.empty, Set.empty)

            val notMarkedEntities = ps.entities(Marker.NotMarked, Marker.NotMarked).toSet
            notMarkedEntities should be(Set("fd", "zu", "aaabbbaaa"))
            ps.finalEntities(Marker.NotMarked).toSet should be(notMarkedEntities)

            afterAll(ps)
            ps.shutdown()
        }

        it("should not set an entity's property if it already has a property") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.set("aba", Palindrome)
            assertThrows[IllegalStateException] { ps.set("aba", NoPalindrome) }

            afterAll(ps)
            ps.shutdown()
        }

        it("should contain properties (at least) for all entities for which we performed a computation") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

            val pk = Palindromes.PalindromeKey
            val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
            ps.scheduleEagerComputationsForEntities(es) { e =>
                Result(e, if (e.reverse == e) Palindrome else NoPalindrome)
            }

            ps.waitOnPhaseCompletion()
            ps.setupPhase(Set.empty, Set.empty)

            ps.entities(pk).map(_.e).toSet should be(es)
            ps.entities(_.lb == Palindrome).toSet should be(Set("aba", "cc", "d", "aaabbbaaa"))
            ps.entities(_.ub == NoPalindrome).toSet should be(Set("fd", "zu"))
            ps.entities(pk).toSet should be(Set(
                FinalEP("aba", Palindrome),
                FinalEP("cc", Palindrome),
                FinalEP("d", Palindrome),
                FinalEP("fd", NoPalindrome),
                FinalEP("zu", NoPalindrome),
                FinalEP("aaabbbaaa", Palindrome)
            ))
            ps.finalEntities(Palindrome).toSet should be(Set("aba", "cc", "d", "aaabbbaaa"))
            ps.finalEntities(NoPalindrome).toSet should be(Set("fd", "zu"))

            es.foreach { e =>
                val expected = if (e.reverse == e) Palindrome else NoPalindrome
                ps.properties(e).map(_.ub).toSet should be(Set(expected))
                ps.properties(e).map(_.lb).toSet should be(Set(expected))
            }

            afterAll(ps)
            ps.shutdown()
        }

        it("should trigger a lazy property computation only lazily") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            val pk = Palindromes.PalindromeKey
            ps.setupPhase(Set(pk), Set.empty)
            ps.registerLazyPropertyComputation(
                pk,
                (e: Entity) => {
                    val p =
                        if (e.toString.reverse == e.toString)
                            Palindrome
                        else
                            NoPalindrome
                    Result(e, p)
                }
            )

            ps.execute {
                if (ps.MaxEvaluationDepth == 0)
                    ps("aba", pk) should be(EPK("aba", pk))
                else
                    ps("aba", pk) should be(FinalEP("aba", Palindrome))
            }

            ps.waitOnPhaseCompletion()
            ps.setupPhase(Set.empty, Set.empty) // <= not strictly required, but a best practice

            ps("aba", pk) should be(FinalEP("aba", Palindrome))

            afterAll(ps)
            ps.shutdown()
        }

        it("should not trigger a lazy property computation multiple times") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            val pk = Palindromes.PalindromeKey
            ps.setupPhase(Set(pk), Set.empty)

            val invocationCount = new AtomicInteger(0)
            ps.registerLazyPropertyComputation(
                pk,
                (e: Entity) => {
                    invocationCount.incrementAndGet()
                    val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                    Result(e, p)
                }
            )
            if (ps.MaxEvaluationDepth == 0) {
                ps("aba", pk) should be(EPK("aba", pk))
                ps("aba", pk) // just trigger it again...
                ps("aba", pk) // just trigger it again...
            } else {
                ps("aba", pk) should be(FinalEP("aba", Palindrome))
            }

            ps.waitOnPhaseCompletion()
            ps.setupPhase(Set.empty, Set.empty) // <= not strictly required, but a best practice

            if (invocationCount.get != 1) {
                fail(s"invocation count should be 1; was ${invocationCount.get}")
            }

            afterAll(ps)
            ps.shutdown()
        }

        {
            val ps = createPropertyStore()
            if (ps.MaxEvaluationDepth == 0) {
                it("should complete the computation of dependent lazy computations "+
                    "before the phase ends") {

                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.set("dummyymmud", Palindrome)
                    ps.set("dummyBADymmud", NoPalindrome)
                    ps.set("dummyymmud", SuperPalindrome)
                    ps.set("dummyBADymmud", NoSuperPalindrome)

                    ps.setupPhase(Set(PalindromeKey, SuperPalindromeKey, Marker.Key), Set.empty)

                    val invocationCount = new AtomicInteger(0)
                    ps.registerLazyPropertyComputation(
                        PalindromeKey,
                        (e: Entity) => {
                            invocationCount.incrementAndGet()
                            val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                            Result(e, p)
                        }
                    )
                    ps.registerLazyPropertyComputation(
                        SuperPalindromeKey,
                        (e: Entity) => {
                            invocationCount.incrementAndGet()
                            val initialSomeEOptionP = ps("e", PalindromeKey)
                            initialSomeEOptionP should be(EPK("e", PalindromeKey))

                            InterimResult(
                                e, NoSuperPalindrome, SuperPalindrome,
                                Set(initialSomeEOptionP),
                                eps => {
                                    if (eps.lb == Palindrome /*&& ...*/ )
                                        Result(e, SuperPalindrome)
                                    else
                                        Result(e, NoSuperPalindrome)
                                }
                            )
                        }
                    )
                    ps.scheduleEagerComputationForEntity("e") { e: String =>
                        val initialSomeEOptionP = ps("e", SuperPalindromeKey)
                        initialSomeEOptionP should be(EPK("e", SuperPalindromeKey))
                        InterimResult(
                            "e", Marker.NotMarked, Marker.IsMarked,
                            Set(initialSomeEOptionP),
                            eps => {
                                // Depending on the scheduling, we can have a final result here as well.
                                if (eps.isFinal) {
                                    if (eps.lb == SuperPalindrome)
                                        Result(e, Marker.IsMarked)
                                    else
                                        Result(e, Marker.NotMarked)
                                } else
                                    InterimResult(
                                        "e", Marker.NotMarked, Marker.IsMarked,
                                        Set(eps),
                                        eps => {
                                            if (!eps.isFinal)
                                                fail("unexpected non final value")

                                            if (eps.lb == SuperPalindrome)
                                                Result(e, Marker.IsMarked)
                                            else
                                                Result(e, Marker.NotMarked)
                                        }
                                    )
                            }
                        )
                    }
                    ps.waitOnPhaseCompletion()

                    ps.setupPhase(Set.empty, Set.empty) // <= not strictly required, but a best practice

                    ps("e", PalindromeKey) should be(FinalEP("e", Palindrome))
                    ps("e", SuperPalindromeKey) should be(FinalEP("e", SuperPalindrome))
                    ps("e", Marker.Key) should be(FinalEP("e", Marker.IsMarked))

                    ps("dummyymmud", PalindromeKey) should be(FinalEP("dummyymmud", Palindrome))
                    ps("dummyBADymmud", PalindromeKey) should be(FinalEP("dummyBADymmud", NoPalindrome))

                    ps("dummyymmud", SuperPalindromeKey) should be(
                        FinalEP("dummyymmud", SuperPalindrome)
                    )
                    ps("dummyBADymmud", SuperPalindromeKey) should be(
                        FinalEP("dummyBADymmud", NoSuperPalindrome)
                    )
                }
            }
            afterAll(ps)
            ps.shutdown()
        }

        it("should correctly handle PartialResults") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.set("aBa", Palindrome)
            ps.set("aNOa", NoPalindrome)

            val processedStrings = scala.collection.concurrent.TrieMap.empty[String, Boolean]

            ps.setupPhase(Set(PalindromeKey, PalindromeFragmentsKey), Set.empty)

            def uc(
                e: String
            )(
                fragmentsEOptionP: EOptionP[Entity, PalindromeFragments]
            ): Option[InterimEP[String, PalindromeFragments]] = {
                processedStrings.put(e, true)
                (fragmentsEOptionP: @unchecked) match {
                    case _: EPK[_, _] =>
                        Some(
                            InterimEUBP(
                                "fragments",
                                PalindromeFragments(Set(e.substring(0, 1)))
                            )
                        )
                    case InterimUBP(PalindromeFragments(fs)) =>
                        val newFs = fs + e.substring(0, 1)
                        if (newFs != fs)
                            Some(InterimEUBP("fragments", PalindromeFragments(newFs)))
                        else
                            None
                }
            }
            ps.registerTriggeredComputation(
                PalindromeKey,
                (e: Entity) => PartialResult("fragments", PalindromeFragmentsKey, uc(e.toString))
            )

            ps.scheduleEagerComputationsForEntities(List("eBe", "eNOe"))(
                (e: Entity) => {
                    val s = e.toString
                    Result(e, if (s.reverse == s) Palindrome else NoPalindrome)
                }
            )

            ps.waitOnPhaseCompletion()
            ps.setupPhase(Set.empty, Set.empty) // <= not strictly required, but a best practice

            ps("fragments", PalindromeFragmentsKey) should be(
                FinalEP("fragments", PalindromeFragments(Set("a", "e")))
            )
            processedStrings.keySet should be(Set("aNOa", "eNOe", "aBa", "eBe"))

            afterAll(ps)
            ps.shutdown()
        }

        it(
            "should be possible to register multiple triggered computations, "+
                "even if the first triggered computation is already potentially triggered"
        ) {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.set("aBa", Palindrome)
                ps.set("aNOa", NoPalindrome)

                val processedStrings = scala.collection.concurrent.TrieMap.empty[String, Boolean]

                ps.setupPhase(Set(PalindromeKey, PalindromeFragmentsKey), Set.empty)

                def uc(
                    e: String
                )(
                    fragmentsEOptionP: EOptionP[Entity, PalindromeFragments]
                ): Option[InterimEP[String, PalindromeFragments]] = {
                    processedStrings.put(e, true)
                    (fragmentsEOptionP: @unchecked) match {
                        case _: EPK[_, _] =>
                            Some(
                                InterimEUBP(
                                    "fragments",
                                    PalindromeFragments(Set(e.substring(0, 1)))
                                )
                            )
                        case InterimUBP(PalindromeFragments(fs)) =>
                            val newFs = fs + e.substring(0, 1)
                            if (newFs != fs)
                                Some(InterimEUBP("fragments", PalindromeFragments(newFs)))
                            else
                                None
                    }
                }
                ps.registerTriggeredComputation(
                    PalindromeKey,
                    (e: Entity) => PartialResult("fragments", PalindromeFragmentsKey, uc(e.toString))
                )

                // In this case, we can actually schedule the same computation multiple times;
                // it should be idempotent anyway!
                ps.registerTriggeredComputation(
                    PalindromeKey,
                    (e: Entity) => PartialResult("fragments", PalindromeFragmentsKey, uc(e.toString))
                )

                ps.waitOnPhaseCompletion()
                ps.setupPhase(Set.empty, Set.empty) // <= not strictly required, but a best practice

                ps("fragments", PalindromeFragmentsKey) should be(
                    FinalEP("fragments", PalindromeFragments(Set("a")))
                )
                processedStrings.keySet should be(Set("aNOa", "aBa"))

                afterAll(ps)
                ps.shutdown()
            }

        describe("handling of computations with multiple updates") {

            // DESCRIPTION OF A GRAPH (WITH CYCLES)
            val nodeA = Node("a")
            val nodeB = Node("b")
            val nodeC = Node("c")
            val nodeD = Node("d")
            val nodeE = Node("e")
            val nodeF = Node("f")
            val nodeG = Node("g")
            val nodeH = Node("h")
            val nodeI = Node("i")
            val nodeJ = Node("j")
            val nodeR = Node("r")
            nodeA.targets += nodeB // the graph:
            nodeA.targets += nodeF // a -> f
            nodeF.targets += nodeH //      f -> h
            nodeA.targets += nodeG // a -> g
            nodeG.targets += nodeH //      g -> h
            nodeA.targets += nodeH // a -> h
            nodeF.targets += nodeJ // f -> j
            nodeF.targets += nodeI // f -> i
            nodeH.targets += nodeJ // h <-> j
            nodeJ.targets += nodeH // ...
            nodeJ.targets += nodeI // j <-> i
            nodeI.targets += nodeJ // ...
            nodeB.targets += nodeC // a -> b -> c
            nodeB.targets += nodeD //        ↘︎  d
            nodeD.targets += nodeD //           d ⟲
            nodeD.targets += nodeE //           d -> e
            nodeE.targets += nodeR //                e -> r
            nodeR.targets += nodeB //       ↖︎------------↵︎
            val nodeEntities = mutable.Set[Node](
                nodeA, nodeB, nodeC, nodeD, nodeE, nodeF, nodeG, nodeH, nodeI, nodeJ, nodeR
            )
            // val nodeEntitiesJustCycle = Set[Node](nodeB, nodeC, nodeD, nodeE, nodeR)

            object ReachableNodes {
                val Key: PropertyKey[ReachableNodes] =
                    PropertyKey.create[Node, ReachableNodes](
                        s"ReachableNodes(t=${System.nanoTime()})",
                        (_: PropertyStore, reason: FallbackReason, e: Node) => AllNodes
                    )
            }
            case class ReachableNodes(nodes: collection.Set[Node]) extends OrderedProperty {
                type Self = ReachableNodes

                def key: PropertyKey[ReachableNodes] = ReachableNodes.Key

                def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
                    if (!this.nodes.subsetOf(other.nodes)) {
                        throw new IllegalArgumentException(
                            s"$e: $this is not equal or better than $other"
                        )
                    }
                }
            }
            object NoReachableNodes extends ReachableNodes(mutable.Set.empty[Node]) {
                override def toString: String = "NoReachableNodes"
            }
            object AllNodes extends ReachableNodes(nodeEntities) {
                override def toString: String = "AllNodes"
            }
            val RNCKey = ReachableNodesCount.Key
            val RNKey = ReachableNodes.Key

            // The following analysis only uses the new information given to it and updates
            // the set of observed dependees.
            def reachableNodesAnalysis(
                ps: PropertyStore
            )(
                n: Node
            ): ProperPropertyComputationResult = {
                val nTargets = n.targets
                if (nTargets.isEmpty)
                    return Result(n, NoReachableNodes);

                var allDependees: immutable.Set[Node] = nTargets.toSet // may include self-dependency
                var dependeePs: Set[EOptionP[Entity, _ <: ReachableNodes]] =
                    ps(allDependees - n /* ignore self-dependency */ , ReachableNodes.Key).toSet

                // incremental computation
                def c(dependeeP: SomeEPS): ProperPropertyComputationResult = {
                    // Get the set of currently reachable nodes:
                    val EUBPS(dependeeE, ReachableNodes(depeendeeReachableNodes), _) = dependeeP

                    // Compute the new set of reachable nodes:
                    allDependees ++= depeendeeReachableNodes
                    val newUB = ReachableNodes(allDependees)

                    // Adapt the set of dependeePs to ensure termination
                    dependeePs = dependeePs filter { _.e ne dependeeE }
                    if (!dependeeP.isFinal) {
                        dependeePs += dependeeP.asInstanceOf[EOptionP[Entity, _ <: ReachableNodes]]
                    }
                    val r = {
                        if (dependeePs.nonEmpty)
                            InterimResult(n, AllNodes, newUB, dependeePs.asInstanceOf[Set[SomeEOptionP]], c)
                        else
                            Result(n, newUB)
                    }
                    r
                }

                // initial computation
                dependeePs foreach { dependeeP =>
                    if (dependeeP.isEPS) {
                        if (dependeeP.isFinal) {
                            dependeePs -= dependeeP
                        }
                        allDependees ++= dependeeP.ub.nodes
                    }
                }
                val currentReachableNodes = ReachableNodes(allDependees)
                if (dependeePs.isEmpty)
                    Result(n, currentReachableNodes)
                else
                    InterimResult(n, AllNodes, currentReachableNodes, dependeePs.asInstanceOf[Set[SomeEOptionP]], c)
            }

            // Expected to be scheduled as an eager analysis for all nodes...
            def reachableNodesAnalysisUsingInterimPartialResults(
                ps: PropertyStore
            )(
                n: Node
            ): ProperPropertyComputationResult = {
                val nTargets = n.targets
                if (nTargets.isEmpty)
                    return Result(n, NoReachableNodes);

                var dependeePs: immutable.Set[EOptionP[Entity, _ <: ReachableNodes]] =
                    ps(nTargets diff (Set(n)) /* ignore self-dependency */ , ReachableNodes.Key)
                        .filter { dependeeP => dependeeP.isRefinable }.toSet

                def createPartialResult(currentNodes: collection.Set[Node]): SomePartialResult = {
                    PartialResult(
                        n,
                        ReachableNodes.Key,
                        (eOptionP: EOptionP[_, ReachableNodes]) =>
                            eOptionP match {
                                case _: EPK[_, _] => Some(InterimEUBP(n, ReachableNodes(currentNodes)))
                                case InterimUBP(p) =>
                                    val oldReachableNodes = p.nodes
                                    val newReachableNodes = oldReachableNodes ++ currentNodes
                                    if (oldReachableNodes != newReachableNodes) {
                                        Some(InterimEUBP(n, ReachableNodes(newReachableNodes)))
                                    } else {
                                        None
                                    }
                                case _ =>
                                    ???
                            }
                    )
                }

                def c(dependeeP: SomeEPS): ProperPropertyComputationResult = {
                    // Get the set of currently reachable nodes of the dependee:
                    val EUBP(dependeeE, ReachableNodes(depeendeeReachableNodes)) = dependeeP
                    // Compute the new set of reachable nodes
                    val pr = createPartialResult(depeendeeReachableNodes)
                    dependeePs = dependeePs.filter(_.e ne dependeeP.e)
                    if (dependeeP.isRefinable)
                        dependeePs += dependeeP.asInstanceOf[EOptionP[Entity, _ <: ReachableNodes]]
                    InterimPartialResult(List(pr), dependeePs.asInstanceOf[Set[SomeEOptionP]], c)
                }

                val allNodes =
                    ps(nTargets, ReachableNodes.Key).foldLeft(immutable.Set.empty[Node]) { (c, n) =>
                        n match {
                            case epk: EPK[Node, _] => c + epk.e
                            case InterimEUBP(n, p) => c ++ p.nodes + n
                            case FinalEP(n, p)     => immutable.Set.from(p.nodes) + n
                        }
                    }

                if (dependeePs.isEmpty /* (here)<=> we just have a self-dependency*/ )
                    Result(n, ReachableNodes(allNodes))
                else {
                    val pr = createPartialResult(allNodes)
                    InterimPartialResult(List(pr), dependeePs.asInstanceOf[Set[SomeEOptionP]], c)
                }
            }

            class ReachableNodesCountAnalysis(
                    val ps: PropertyStore
            ) extends (Node => ProperPropertyComputationResult) {

                def apply(n: Node): ProperPropertyComputationResult = {
                    var dependees: Set[SomeEOptionP] = Set.empty
                    var ub: Int = n.targets.size

                    def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                        (eps: @unchecked) match {
                            case InterimUBP(ReachableNodesCount(otherUB)) =>
                                if (ub + otherUB > 4)
                                    Result(n, TooManyNodesReachable)
                                else {
                                    dependees = dependees.filter(_.e ne eps.e) + eps
                                    InterimResult(
                                        n, TooManyNodesReachable, ReachableNodesCount(ub),
                                        dependees,
                                        c
                                    )
                                }

                            case FinalP(reachableNodesCount: ReachableNodesCount) =>
                                ub += reachableNodesCount.value
                                if (ub > 4) {
                                    Result(n, TooManyNodesReachable)
                                } else if (dependees.tail.isEmpty) {
                                    Result(n, ReachableNodesCount(ub))
                                } else {
                                    dependees = dependees.filter(_.e ne eps.e)
                                    InterimResult(
                                        n, TooManyNodesReachable, ReachableNodesCount(ub),
                                        dependees,
                                        c
                                    )
                                }
                        }
                    }

                    n.targets forall { successor =>
                        if (successor == n) {
                            ub = TooManyNodesReachable.value
                            false // we are done...
                        } else {
                            ps(successor, ReachableNodesCount.Key) match {
                                case epk: EPK[_, _] =>
                                    dependees += epk
                                    true
                                case iep @ InterimUBP(ReachableNodesCount(otherUB)) =>
                                    if (ub + otherUB > 4) {
                                        ub = TooManyNodesReachable.value
                                        false
                                    } else {
                                        // we have to wait for the final value before we can
                                        // add the count
                                        dependees += iep
                                        true
                                    }
                                case FinalP(reachableNodesCount) =>
                                    ub += reachableNodesCount.value
                                    true
                            }
                        }
                    }
                    if (ub > 4)
                        Result(n, TooManyNodesReachable)
                    else if (dependees.isEmpty)
                        Result(n, ReachableNodesCount(ub))
                    else
                        InterimResult(
                            n, TooManyNodesReachable, ReachableNodesCount(ub), dependees, c
                        )
                }

                override def toString(): String = {
                    "ReachableNodesCountAnalysis@"+System.identityHashCode(this).toHexString
                }
            }

            class ReachableNodesCountViaReachableNodesAnalysis(
                    val ps: PropertyStore
            ) extends (Node => ProperPropertyComputationResult) {
                def apply(n: Node): ProperPropertyComputationResult = {

                    def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                        (eps: @unchecked) match {
                            case eps @ InterimUBP(ReachableNodes(nodes)) =>
                                val lb = TooManyNodesReachable
                                val ub = ReachableNodesCount(nodes.size)
                                InterimResult(n, lb, ub, Set(eps), c)

                            case FinalP(ReachableNodes(nodes)) =>
                                Result(n, ReachableNodesCount(nodes.size))
                        }
                    }

                    ps(n, ReachableNodes.Key) match {
                        case epk: EPK[_, _] =>
                            val lb = TooManyNodesReachable
                            val ub = ReachableNodesCount(0)
                            InterimResult(n, lb, ub, Set(epk), c)

                        case eps: SomeEOptionP =>
                            c(eps)

                    }
                }

                override def toString(): String = {
                    "ReachableNodesCountViaReachableNodesAnalysis@"+
                        System.identityHashCode(this).toHexString
                }
            }

            class ReachedEntitiesAnalysis extends (Entity => SomePartialResult) {

                def apply(e: Entity): SomePartialResult = PartialResult(
                    "reached nodes",
                    ReachedEntitiesKey,
                    (eOptionP: EOptionP[_, ReachedEntities]) =>
                        (eOptionP) match {
                            case _: EPK[_, _] =>
                                Some(InterimEUBP("reached nodes", ReachedEntities(Set(e.toString))))
                            case InterimUBP(p) =>
                                val oldEntities = p.entities
                                val newEntities = oldEntities + e.toString
                                if (oldEntities ne newEntities)
                                    Some(InterimEUBP("reached nodes", ReachedEntities(newEntities)))
                                else
                                    None
                            case _ =>
                                throw new UnknownError
                        }
                )

                override def toString(): String = {
                    "ReachedEntitiesAnalysis@"+System.identityHashCode(this).toHexString
                }
            }

            // the graph:
            // a -> f -> h
            // a -> f -> j
            // a -> f -> i
            //           h <-> j <-> i // this cSCC is a chain..
            // a -> g -> h
            // a -> h
            // a -> b -> c
            //      b -> d
            //           d ⟲
            //           d -> e
            //                e -> r
            //       ↖︎-----------< r
            it("should be possible using eagerly scheduled computations") {
                val dropCount = (System.nanoTime() % 100000).toInt
                var count = -1
                val nodeEntitiesPermutations = nodeEntities.toList.permutations
                for {
                    nodeEntitiesPermutations <- List(
                        nodeEntitiesPermutations.slice(dropCount, dropCount + 1000).toList,
                        nodeEntitiesPermutations.slice(dropCount, dropCount + 1000).toList,
                        nodeEntitiesPermutations.slice(dropCount, dropCount + 1000).toList,
                        nodeEntitiesPermutations.slice(dropCount, dropCount + 1000).toList,
                        nodeEntitiesPermutations.slice(dropCount, dropCount + 1000).toList
                    )
                    nodeEntitiesPermutation <- nodeEntitiesPermutations
                } {
                    count += 1
                    if (count % 100 == 0) {
                        val ps = createPropertyStore()
                        info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                        ps.setupPhase(
                            Set(ReachableNodesCount.Key, ReachableNodes.Key, ReachedEntitiesKey),
                            Set.empty
                        )
                        ps.registerLazyPropertyComputation(
                            RNCKey, new ReachableNodesCountAnalysis(ps)
                        )
                        ps.registerTriggeredComputation(
                            ReachableNodes.Key,
                            new ReachedEntitiesAnalysis
                        )
                        ps.scheduleEagerComputationsForEntities(nodeEntitiesPermutation)(reachableNodesAnalysis(ps))
                        ps.force(nodeA, ReachableNodesCount.Key) // forces the evaluation of nodes count for all nodes...

                        ps.waitOnPhaseCompletion()
                        ps.setupPhase(Set.empty, Set.empty)

                        info(
                            s"(id of first permutation = ${dropCount + 1}; this permutation="+
                                s"${nodeEntitiesPermutation.mkString("[", ",", "]")} "+
                                "; number of executed tasks:"+ps.scheduledTasksCount+
                                "; number of scheduled onUpdateContinuations:"+
                                ps.scheduledOnUpdateComputationsCount
                        )
                        try {
                            ps("reached nodes", ReachedEntitiesKey) should be(
                                FinalEP(
                                    "reached nodes",
                                    ReachedEntities(nodeEntities.toSet.map((n: Node) => n.toString))
                                )
                            )
                            ps(nodeA, ReachableNodes.Key) should be(FinalEP(
                                nodeA, ReachableNodes(nodeEntities.toSet - nodeA)
                            ))
                            ps(nodeB, ReachableNodes.Key) should be(FinalEP(
                                nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                            ps(nodeC, ReachableNodes.Key) should be(FinalEP(
                                nodeC, ReachableNodes(Set())
                            ))
                            ps(nodeD, ReachableNodes.Key) should be(FinalEP(
                                nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                            ps(nodeE, ReachableNodes.Key) should be(FinalEP(
                                nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                            ps(nodeR, ReachableNodes.Key) should be(FinalEP(
                                nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                        } catch {
                            case t: Throwable =>
                                throw t;
                        }

                        afterAll(ps)
                        ps.shutdown()
                    }
                }
            }

            it("should be possible using interim partial results") {
                val dropCount = (System.nanoTime() % 10000).toInt
                var count = -1
                val nodeEntitiesPermutations = nodeEntities.toList.permutations
                for (nodeEntitiesPermutation <- nodeEntitiesPermutations.drop(dropCount).take(1000)) {
                    count += 1
                    if (count % 99 == 0) {
                        val ps = createPropertyStore()
                        info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                        ps.setupPhase(Set(ReachableNodes.Key), Set.empty)
                        ps.scheduleEagerComputationsForEntities(nodeEntitiesPermutation)(
                            reachableNodesAnalysisUsingInterimPartialResults(ps)
                        )
                        ps.waitOnPhaseCompletion()

                        ps.setupPhase(Set.empty, Set.empty)

                        info(
                            s"(id of first permutation = ${dropCount + 1}; this permutation="+
                                s"${nodeEntitiesPermutation.mkString("[", ",", "]")} "+
                                "; number of executed tasks:"+ps.scheduledTasksCount+
                                "; number of scheduled onUpdateContinuations:"+
                                ps.scheduledOnUpdateComputationsCount
                        )
                        try {
                            ps(nodeA, ReachableNodes.Key) should be(FinalEP(
                                nodeA, ReachableNodes(nodeEntities.toSet - nodeA)
                            ))
                            ps(nodeB, ReachableNodes.Key) should be(FinalEP(
                                nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                            ps(nodeC, ReachableNodes.Key) should be(FinalEP(
                                nodeC, ReachableNodes(Set())
                            ))
                            ps(nodeD, ReachableNodes.Key) should be(FinalEP(
                                nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                            ps(nodeE, ReachableNodes.Key) should be(FinalEP(
                                nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                            ps(nodeR, ReachableNodes.Key) should be(FinalEP(
                                nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))
                            ))
                        } catch {
                            case t: Throwable =>
                                throw t;
                        }

                        afterAll(ps)
                        ps.shutdown()
                    }
                }
            }

            it("should be possible using lazily scheduled computations") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodes.Key), Set.empty)
                ps.registerLazyPropertyComputation(
                    ReachableNodes.Key, reachableNodesAnalysis(ps)
                )
                ps.force(nodeA, ReachableNodes.Key) // forces the evaluation for all nodes...
                ps.waitOnPhaseCompletion()

                ps(nodeA, ReachableNodes.Key) should be(
                    FinalEP(nodeA, ReachableNodes(nodeEntities diff Set(nodeA)))
                )
                ps(nodeB, ReachableNodes.Key) should be(
                    FinalEP(nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                )
                ps(nodeC, ReachableNodes.Key) should be(
                    FinalEP(nodeC, ReachableNodes(Set()))
                )
                ps(nodeD, ReachableNodes.Key) should be(
                    FinalEP(nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                )
                ps(nodeE, ReachableNodes.Key) should be(
                    FinalEP(nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                )
                ps(nodeR, ReachableNodes.Key) should be(
                    FinalEP(nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                )

                afterAll(ps)
                ps.shutdown()
            }

            it("should be possible to use lazily scheduled mutually dependent computations") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodes.Key, ReachableNodesCount.Key), Set.empty)
                ps.registerLazyPropertyComputation(
                    ReachableNodes.Key, reachableNodesAnalysis(ps)
                )
                ps.registerLazyPropertyComputation(
                    ReachableNodesCount.Key, new ReachableNodesCountViaReachableNodesAnalysis(ps)
                )
                nodeEntities foreach { node => ps.force(node, ReachableNodesCount.Key) }
                ps.waitOnPhaseCompletion()
                info("scheduledTasksCount="+ps.scheduledTasksCount)
                info("scheduledOnUpdateComputationsCount="+ps.scheduledOnUpdateComputationsCount)

                try {
                    ps(nodeA, ReachableNodes.Key) should be(
                        FinalEP(nodeA, ReachableNodes(nodeEntities.toSet - nodeA))
                    )
                    ps(nodeB, ReachableNodes.Key) should be(
                        FinalEP(nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    ps(nodeC, ReachableNodes.Key) should be(
                        FinalEP(nodeC, ReachableNodes(Set()))
                    )
                    ps(nodeD, ReachableNodes.Key) should be(
                        FinalEP(nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    ps(nodeE, ReachableNodes.Key) should be(
                        FinalEP(nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    ps(nodeR, ReachableNodes.Key) should be(
                        FinalEP(nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    // now let's check if we have the correct notification of the
                    // lazily dependent computations
                    ps(nodeA, ReachableNodesCount.Key) should be(
                        FinalEP(nodeA, ReachableNodesCount(nodeEntities.size - 1))
                    )
                    ps(nodeB, ReachableNodesCount.Key) should be(
                        FinalEP(nodeB, ReachableNodesCount(5))
                    )
                    ps(nodeC, ReachableNodesCount.Key) should be(
                        FinalEP(nodeC, ReachableNodesCount(0))
                    )
                    ps(nodeD, ReachableNodesCount.Key) should be(
                        FinalEP(nodeD, ReachableNodesCount(5))
                    )
                    ps(nodeE, ReachableNodesCount.Key) should be(
                        FinalEP(nodeE, ReachableNodesCount(5))
                    )
                    ps(nodeR, ReachableNodesCount.Key) should be(
                        FinalEP(nodeR, ReachableNodesCount(5))
                    )
                } catch {
                    case t: Throwable =>
                        throw t;
                }

                afterAll(ps)
                ps.shutdown()
            }

            it(
                "should be possible to use lazily scheduled mutually dependent computations "+
                    "without intermediate results propagation"
            ) {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(
                        Set(ReachableNodes.Key, ReachableNodesCount.Key),
                        Set.empty,
                        Map(ReachableNodesCount.Key -> Set(ReachableNodes.Key))
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodes.Key, reachableNodesAnalysis(ps)
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodesCount.Key,
                        new ReachableNodesCountViaReachableNodesAnalysis(ps)
                    )
                    nodeEntities foreach { node => ps.force(node, ReachableNodesCount.Key) }

                    ps.waitOnPhaseCompletion()
                    info("scheduledTasksCount="+ps.scheduledTasksCount)
                    info(
                        "scheduledOnUpdateComputationsCount="+
                            ps.scheduledOnUpdateComputationsCount
                    )

                    val RNKey = ReachableNodes.Key
                    val RNCKey = ReachableNodesCount.Key

                    ps(nodeA, RNKey) should be(
                        FinalEP(nodeA, ReachableNodes(nodeEntities diff Set(nodeA)))
                    )
                    ps(nodeB, RNKey) should be(
                        FinalEP(nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    ps(nodeC, RNKey) should be(
                        FinalEP(nodeC, ReachableNodes(Set()))
                    )
                    ps(nodeD, RNKey) should be(
                        FinalEP(nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    ps(nodeE, RNKey) should be(
                        FinalEP(nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    ps(nodeR, RNKey) should be(
                        FinalEP(nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR)))
                    )
                    // now let's check if we have the correct notification of the
                    // lazily dependent computations
                    ps(nodeA, RNCKey) should be(
                        FinalEP(nodeA, ReachableNodesCount(nodeEntities.size - 1))
                    )
                    ps(nodeB, RNCKey) should be(FinalEP(nodeB, ReachableNodesCount(5)))
                    ps(nodeC, RNCKey) should be(FinalEP(nodeC, ReachableNodesCount(0)))
                    ps(nodeD, RNCKey) should be(FinalEP(nodeD, ReachableNodesCount(5)))
                    ps(nodeE, RNCKey) should be(FinalEP(nodeE, ReachableNodesCount(5)))
                    ps(nodeR, RNCKey) should be(FinalEP(nodeR, ReachableNodesCount(5)))

                    afterAll(ps)
                    ps.shutdown()
                }

            it(
                "should be possible to force the results of transformers "+
                    "based on lazy properties"
            ) {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(
                        Set(ReachableNodes.Key, ReachableNodesCount.Key),
                        Set.empty,
                        Map(ReachableNodesCount.Key -> Set(ReachableNodes.Key))
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodes.Key, reachableNodesAnalysis(ps)
                    )
                    ps.registerTransformer(ReachableNodes.Key, ReachableNodesCount.Key) { (e: Entity, p) =>
                        val ReachableNodes(nodes) = p
                        FinalEP(e, ReachableNodesCount(nodes.size))
                    }
                    nodeEntities foreach { node => ps.force(node, ReachableNodesCount.Key) }
                    ps.waitOnPhaseCompletion()
                    info("scheduledTasksCount="+ps.scheduledTasksCount)
                    info("scheduledOnUpdateComputationsCount="+ps.scheduledOnUpdateComputationsCount)

                    ps(nodeA, RNCKey) should be(
                        FinalEP(nodeA, ReachableNodesCount(nodeEntities.size - 1))
                    )
                    ps(nodeB, RNCKey) should be(FinalEP(nodeB, ReachableNodesCount(5)))
                    ps(nodeC, RNCKey) should be(FinalEP(nodeC, ReachableNodesCount(0)))
                    ps(nodeD, RNCKey) should be(FinalEP(nodeD, ReachableNodesCount(5)))
                    ps(nodeE, RNCKey) should be(FinalEP(nodeE, ReachableNodesCount(5)))
                    ps(nodeR, RNCKey) should be(FinalEP(nodeR, ReachableNodesCount(5)))

                    afterAll(ps)
                    ps.shutdown()
                }

            it(
                "should be possible to force the results of "+
                    "multiple transformers based on a lazy property"
            ) {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(
                        Set(ReachableNodes.Key, ReachableNodesCount.Key, Marker.Key),
                        Set.empty,
                        Map(
                            ReachableNodesCount.Key -> Set(ReachableNodes.Key),
                            Marker.Key -> Set(ReachableNodes.Key)
                        )
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodes.Key, reachableNodesAnalysis(ps)
                    )
                    ps.registerTransformer(RNKey, RNCKey) { (e: Entity, p) =>
                        val ReachableNodes(nodes) = p
                        FinalEP(e, ReachableNodesCount(nodes.size))
                    }
                    ps.registerTransformer(RNKey, Marker.Key) { (e: Entity, p) =>
                        val ReachableNodes(nodes) = p
                        FinalEP(e, if (nodes.size > 3) Marker.IsMarked else Marker.NotMarked)
                    }
                    nodeEntities foreach { node => ps.force(node, Marker.Key) }
                    ps.waitOnPhaseCompletion()
                    info("scheduledTasksCount="+ps.scheduledTasksCount)
                    info("scheduledOnUpdateComputationsCount="+ps.scheduledOnUpdateComputationsCount)

                    ps(nodeA, RNCKey) should be(
                        FinalEP(nodeA, ReachableNodesCount(nodeEntities.size - 1))
                    )
                    ps(nodeB, RNCKey) should be(FinalEP(nodeB, ReachableNodesCount(5)))
                    ps(nodeC, RNCKey) should be(FinalEP(nodeC, ReachableNodesCount(0)))
                    ps(nodeD, RNCKey) should be(FinalEP(nodeD, ReachableNodesCount(5)))
                    ps(nodeE, RNCKey) should be(FinalEP(nodeE, ReachableNodesCount(5)))
                    ps(nodeR, RNCKey) should be(FinalEP(nodeR, ReachableNodesCount(5)))

                    val MKey = Marker.Key
                    ps(nodeA, MKey) should be(FinalEP(nodeA, Marker.IsMarked))
                    ps(nodeB, MKey) should be(FinalEP(nodeB, Marker.IsMarked))
                    ps(nodeC, MKey) should be(FinalEP(nodeC, Marker.NotMarked))
                    ps(nodeD, MKey) should be(FinalEP(nodeD, Marker.IsMarked))
                    ps(nodeE, MKey) should be(FinalEP(nodeE, Marker.IsMarked))
                    ps(nodeR, MKey) should be(FinalEP(nodeR, Marker.IsMarked))

                    afterAll(ps)
                    ps.shutdown()
                }

            it(
                "should be possible to force the results of "+
                    "a chain of transformers based on lazy properties"
            ) {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(
                        Set(ReachableNodes.Key, ReachableNodesCount.Key, Marker.Key),
                        Set.empty,
                        Map(
                            ReachableNodesCount.Key -> Set(ReachableNodes.Key),
                            Marker.Key -> Set(ReachableNodesCount.Key)
                        )
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodes.Key, reachableNodesAnalysis(ps)
                    )
                    ps.registerTransformer(RNKey, RNCKey) { (e: Entity, p) =>
                        val ReachableNodes(nodes) = p
                        FinalEP(e, ReachableNodesCount(nodes.size))
                    }
                    ps.registerTransformer(RNCKey, Marker.Key) { (e: Entity, p) =>
                        val ReachableNodesCount(count) = p
                        FinalEP(e, if (count > 3) Marker.IsMarked else Marker.NotMarked)
                    }
                    nodeEntities foreach { node => ps.force(node, Marker.Key) }
                    ps.waitOnPhaseCompletion()
                    info("scheduledTasksCount="+ps.scheduledTasksCount)
                    info("scheduledOnUpdateComputationsCount="+ps.scheduledOnUpdateComputationsCount)
                    val MKey = Marker.Key
                    ps(nodeA, MKey) should be(FinalEP(nodeA, Marker.IsMarked))
                    ps(nodeB, MKey) should be(FinalEP(nodeB, Marker.IsMarked))
                    ps(nodeC, MKey) should be(FinalEP(nodeC, Marker.NotMarked))
                    ps(nodeD, MKey) should be(FinalEP(nodeD, Marker.IsMarked))
                    ps(nodeE, MKey) should be(FinalEP(nodeE, Marker.IsMarked))
                    ps(nodeR, MKey) should be(FinalEP(nodeR, Marker.IsMarked))

                    afterAll(ps)
                    ps.shutdown()
                }

            it(
                "should be possible to force the results of a lazy property which depends "+
                    "on the results of a transformer backed by an eager analysis"
            ) {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(
                        Set(ReachableNodes.Key, ReachableNodesCount.Key, Marker.Key),
                        Set.empty,
                        Map(
                            ReachableNodesCount.Key -> Set(ReachableNodes.Key),
                            Marker.Key -> Set(ReachableNodesCount.Key)
                        )
                    )
                    ps.registerTransformer(RNKey, RNCKey) { (e: Entity, p) =>
                        val ReachableNodes(nodes) = p
                        FinalEP(e, ReachableNodesCount(nodes.size))
                    }
                    def handleEOptionP(eOptionP: SomeEOptionP): ProperPropertyComputationResult = {
                        eOptionP match {
                            case FinalP(ReachableNodesCount(value)) =>
                                val p = if (value > 3) Marker.IsMarked else Marker.NotMarked
                                Result(eOptionP.e, p)
                            case eOptionP =>
                                val interimELBP = InterimELBP(eOptionP.e, Marker.NotMarked)
                                InterimResult(interimELBP, Set(eOptionP), handleEOptionP)
                        }
                    }
                    ps.registerLazyPropertyComputation(
                        Marker.Key,
                        (e: Entity) => handleEOptionP(ps(e, RNCKey))
                    )
                    ps.scheduleEagerComputationsForEntities(nodeEntities)(reachableNodesAnalysis(ps))
                    nodeEntities foreach { node => ps.force(node, Marker.Key) }
                    ps.waitOnPhaseCompletion()
                    info("scheduledTasksCount="+ps.scheduledTasksCount)
                    info("scheduledOnUpdateComputationsCount="+ps.scheduledOnUpdateComputationsCount)
                    val MKey = Marker.Key
                    ps.get(nodeA, MKey) should be(Some(FinalEP(nodeA, Marker.IsMarked)))
                    ps.get(nodeB, MKey) should be(Some(FinalEP(nodeB, Marker.IsMarked)))
                    ps.get(nodeC, MKey) should be(Some(FinalEP(nodeC, Marker.NotMarked)))
                    ps.get(nodeD, MKey) should be(Some(FinalEP(nodeD, Marker.IsMarked)))
                    ps.get(nodeE, MKey) should be(Some(FinalEP(nodeE, Marker.IsMarked)))
                    ps.get(nodeR, MKey) should be(Some(FinalEP(nodeR, Marker.IsMarked)))

                    afterAll(ps)
                    ps.shutdown()
                }

            it(
                "should be possible when a lazy computation depends on properties "+
                    "for which no analysis is scheduled"
            ) {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)
                    // WE DO NOT SCHEDULE ReachableNodes
                    ps.registerLazyPropertyComputation(
                        ReachableNodesCount.Key, new ReachableNodesCountViaReachableNodesAnalysis(ps)
                    )
                    nodeEntities.foreach { node => ps.force(node, ReachableNodesCount.Key) }
                    ps.waitOnPhaseCompletion()

                    // actually, the "fallback" value
                    ps(nodeA, ReachableNodes.Key) should be(
                        FinalEP(nodeA, ReachableNodes(nodeEntities.toSet))
                    )

                    // now let's check if we have the correct notification of the
                    // of the lazily dependent computations
                    val expected = ReachableNodesCount(11)
                    ps(nodeA, ReachableNodesCount.Key) should be(FinalEP(nodeA, expected))
                    ps(nodeB, ReachableNodesCount.Key) should be(FinalEP(nodeB, expected))
                    ps(nodeC, ReachableNodesCount.Key) should be(FinalEP(nodeC, expected))
                    ps(nodeD, ReachableNodesCount.Key) should be(FinalEP(nodeD, expected))
                    ps(nodeE, ReachableNodesCount.Key) should be(FinalEP(nodeE, expected))
                    ps(nodeR, ReachableNodesCount.Key) should be(FinalEP(nodeR, expected))

                    afterAll(ps)
                    ps.shutdown()
                }

            it("should be possible that a lazy computation depends on properties "+
                "for which an analysis is seemingly scheduled, "+
                "but no analysis actually produces results") {

                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodesCount.Key, ReachableNodes.Key), Set.empty)
                // WE DO NOT SCHEDULE ReachableNodes
                ps.registerLazyPropertyComputation(
                    ReachableNodesCount.Key, new ReachableNodesCountViaReachableNodesAnalysis(ps)
                )
                nodeEntities.foreach { node => ps.force(node, ReachableNodesCount.Key) }
                ps.waitOnPhaseCompletion()

                // actually, the "fallback" value
                ps(nodeA, ReachableNodes.Key) should be(
                    FinalEP(nodeA, ReachableNodes(nodeEntities.toSet))
                )

                // now let's check if we have the correct notification of the
                // of the lazily dependent computations
                val expected = ReachableNodesCount(11)
                ps(nodeA, ReachableNodesCount.Key) should be(FinalEP(nodeA, expected))
                ps(nodeB, ReachableNodesCount.Key) should be(FinalEP(nodeB, expected))
                ps(nodeC, ReachableNodesCount.Key) should be(FinalEP(nodeC, expected))
                ps(nodeD, ReachableNodesCount.Key) should be(FinalEP(nodeD, expected))
                ps(nodeE, ReachableNodesCount.Key) should be(FinalEP(nodeE, expected))
                ps(nodeR, ReachableNodesCount.Key) should be(FinalEP(nodeR, expected))

                afterAll(ps)
                ps.shutdown()
            }

        }

        it("should be possible to execute an analysis incrementally") {
            import scala.collection.mutable

            class Node(val name: String, val targets: mutable.Set[Node] = mutable.Set.empty) {
                override def hashCode: Int = name.hashCode()

                override def equals(other: Any): Boolean = other match {
                    case that: Node => this.name equals that.name
                    case _          => false
                }

                override def toString: String = name
            }
            object Node {
                def apply(name: String) = new Node(name)
            }

            // DESCRIPTION OF A TREE
            val nodeRoot = Node("Root")
            val nodeLRoot = Node("Root->L")
            val nodeLLRoot = Node("Root->L->L")
            val nodeRRoot = Node("Root->R")
            val nodeLRRoot = Node("Root->R->L")
            val nodeRRRoot = Node("Root->R->R")
            nodeRoot.targets += nodeLRoot
            nodeRoot.targets += nodeRRoot
            nodeLRoot.targets += nodeLLRoot
            nodeRRoot.targets += nodeLRRoot
            nodeRRoot.targets += nodeRRRoot

            val TreeLevelKey: PropertyKey[TreeLevel] = {
                PropertyKey.create(
                    s"TreeLevel(t=${System.nanoTime()}",
                    (ps: PropertyStore, reason: FallbackReason, e: Entity) => ???
                )
            }
            case class TreeLevel(length: Int) extends Property {
                final type Self = TreeLevel

                final def key = TreeLevelKey
            }

            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(TreeLevelKey), Set.empty)

            /* The following analysis only uses the new information given to it and updates
                 * the set of observed dependees.
                 */
            def analysis(level: Int)(n: Node): PropertyComputationResult = {
                val nextPCs = n.targets.map(t => (analysis(level + 1) _, t)).iterator
                IncrementalResult(Result(n, TreeLevel(level)), nextPCs)
            }

            ps.scheduleEagerComputationForEntity(nodeRoot)(analysis(0))
            ps.waitOnPhaseCompletion()

            ps(nodeRoot, TreeLevelKey) should be(FinalEP(nodeRoot, TreeLevel(0)))
            ps(nodeRRoot, TreeLevelKey) should be(FinalEP(nodeRRoot, TreeLevel(1)))
            ps(nodeRRRoot, TreeLevelKey) should be(FinalEP(nodeRRRoot, TreeLevel(2)))
            ps(nodeLRRoot, TreeLevelKey) should be(FinalEP(nodeLRRoot, TreeLevel(2)))
            ps(nodeLRoot, TreeLevelKey) should be(FinalEP(nodeLRoot, TreeLevel(1)))
            ps(nodeLLRoot, TreeLevelKey) should be(FinalEP(nodeLLRoot, TreeLevel(2)))

            afterAll(ps)
            ps.shutdown()
        }

        it("should never pass a null property to clients") {
            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            val ppk = Palindromes.PalindromeKey
            ps.setupPhase(Set(ppk), Set.empty)

            ps.registerLazyPropertyComputation(
                ppk,
                (e: Entity) => {
                    val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                    Result(e, p)
                }
            )
            ps.scheduleEagerComputationForEntity("aaa") { s: String =>
                ps("a", ppk) match {
                    case epk: EPK[_, _] =>
                        InterimResult(
                            s, NoPalindrome, Palindrome,
                            Set(epk),
                            (eps: SomeEPS) => {
                                if (eps.lb == null || eps.ub == null)
                                    fail("clients should never see null properties")
                                else
                                    Result(s, Palindrome)
                            }
                        )
                    case _ => fail("unexpected result")
                }
            }

            afterAll(ps)
            ps.shutdown()
        }

        it("should be possible to execute an analysis which analyzes a huge circle") {
            import scala.collection.mutable

            val testSizes = Set(1, 5, 50000)
            for (testSize <- testSizes) {
                // 1. we create a ((very) long) chain
                val firstNode = Node(0.toString)
                val allNodes = mutable.Set(firstNode)
                var prevNode = firstNode
                for { i <- 1 to testSize } {
                    val nextNode = Node(i.toString)
                    allNodes += nextNode
                    prevNode.targets += nextNode
                    prevNode = nextNode
                }
                prevNode.targets += firstNode

                // 2. we create the store
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                // 3. setup
                ps.setupPhase(Set(Purity.Key), Set.empty)
                def purityAnalysis(node: Node): PropertyComputationResult = {
                    def c(successorNode: SomeEOptionP): ProperPropertyComputationResult = {
                        // HERE - For this test case only, we can simple get to the previous
                        // node from the one that was updated.
                        (successorNode: @unchecked) match {
                            case epk: EPK[_, _] =>
                                InterimResult(node, Impure, Pure, Set(epk), c)

                            case eps @ InterimLUBP(lb: Property, ub: Property) =>
                                InterimResult(node, lb, ub, Set(eps), c)

                            // required when we resolve the cycle
                            case FinalP(Pure) => Result(node, Pure)
                        }
                    }

                    val nextNode = node.targets.head // HERE: we always have only one successor
                    c(ps(nextNode, Purity.Key))
                }

                // 4. execute analysis
                ps.scheduleEagerComputationsForEntities(allNodes)(purityAnalysis)
                ps.waitOnPhaseCompletion()

                // 5. let's evaluate the result
                ps.entities(Purity.Key) foreach { eps =>
                    if (eps.lb != Pure) {
                        info(ps.toString(true))
                        fail(s"Node(${eps.e}) is not Pure (${eps.lb})")
                    }
                }

                afterAll(ps)
                ps.shutdown()

                info(s"test succeeded with $testSize node(s) in a circle")
                info(
                    s"number of executed tasks:"+ps.scheduledTasksCount+
                        "; number of scheduled onUpdateContinuations:"+
                        ps.scheduledOnUpdateComputationsCount
                )
            }
        }

        it("should be possible to execute an analysis which analyzes a huge circle "+
            "to compute the upper bound only") {
            import scala.collection.mutable

            val testSizes = Set(1, 5, 50, 1000)
            for (testSize <- testSizes) {
                // 1. we create a ((very) long) chain
                val firstNode = Node(0.toString)
                val allNodes = mutable.Set(firstNode)
                var prevNode = firstNode
                for { i <- 1 to testSize } {
                    val nextNode = Node(i.toString)
                    allNodes += nextNode
                    prevNode.targets += nextNode
                    prevNode = nextNode
                }
                prevNode.targets += firstNode

                // 2. we create the store
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(Purity.Key), Set.empty)

                def purityAnalysis(node: Node): PropertyComputationResult = {
                    def c(successorNode: SomeEOptionP): ProperPropertyComputationResult = {
                        // HERE - for this test case only - we can simply get the previous
                        // node from the one that was updated.
                        (successorNode: @unchecked) match {
                            case epk: EPK[_, _] =>
                                InterimResult.forUB(node, Pure, Set(epk), c)

                            case eps @ InterimUBP(ub: Property) =>
                                InterimResult.forUB(node, ub, Set(eps), c)

                            // HERE, the following is not required - the cycle will be
                            // automatically lifted when we reach "quiescence"
                            // case FinalP(Pure)       => Result(node, Pure)
                        }
                    }

                    val nextNode = node.targets.head // HERE: we always have only one successor
                    c(ps(nextNode, Purity.Key))
                }
                // 4. execute analysis
                ps.scheduleEagerComputationsForEntities(allNodes)(purityAnalysis)
                ps.waitOnPhaseCompletion()

                // 5. let's evaluate the result
                ps.entities(Purity.Key) foreach { eps =>
                    if (eps.ub != Pure) {
                        info(ps.toString(true))
                        fail(s"Node(${eps.e}) is not Pure (${eps.ub})")
                    }
                }

                afterAll(ps)
                ps.shutdown()

                info(s"test succeeded with $testSize node(s) in a circle")
                info(
                    s"number of executed tasks:"+ps.scheduledTasksCount+
                        "; number of scheduled onUpdateContinuations:"+
                        ps.scheduledOnUpdateComputationsCount
                )
            }
        }
    }
}

abstract class PropertyStoreTestWithDebugging[PS <: PropertyStore] extends PropertyStoreTest[PS] {

    private[this] val oldPropertyStoreUpdateSetting = PropertyStore.Debug
    private[this] val oldPropertyStoreTraceFallbacksSetting = PropertyStore.TraceFallbacks

    override def beforeAll(): Unit = {
        PropertyStore.updateDebug(true)
        PropertyStore.updateTraceFallbacks(true)
    }
    override def afterAll(): Unit = {
        PropertyStore.updateDebug(oldPropertyStoreUpdateSetting)
        PropertyStore.updateTraceFallbacks(oldPropertyStoreTraceFallbacksSetting)
    }

    describe(s"the property store with debugging support") {

        it("should catch InterimResults with inverted property bounds") {
            assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

            val nodeA = Node("a")
            val nodeB = Node("b")

            val ps = createPropertyStore()
            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

            ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

            def aAnalysis(ignored: Node): PropertyComputationResult = {
                val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                val lb = ReachableNodesCount(10)
                val invalidUB = ReachableNodesCount(20)
                InterimResult(nodeA, lb, invalidUB, Set(bEOptionP), eps => ???)
            }

            try {
                ps.scheduleEagerComputationForEntity(nodeA)(aAnalysis)
                ps.waitOnPhaseCompletion()
                fail("invalid update not detected")
            } catch {
                case iae: IllegalArgumentException if (
                    iae.getMessage.contains("is not equal or better than")
                ) => // OK - EXPECTED
            }

            afterAll(ps)
            ps.shutdown()
        }

        it("should catch non-monotonic updates related to the lower bound") {
            val ps = createPropertyStore()
            if (ps.MaxEvaluationDepth == 0) {

                assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

                var count = 0
                val nodeA = Node("a")
                val nodeB = Node("b")
                val nodeC = Node("c")
                val nodeD = Node("d")

                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                def c(n: Node)(eOptP: SomeEOptionP): ProperPropertyComputationResult = {
                    n match {
                        case `nodeA` =>
                            Result(nodeA, ReachableNodesCount(50))
                        case `nodeB` =>
                            InterimResult(
                                n,
                                ReachableNodesCount(100), // <= invalid refinement of lower bound!
                                ReachableNodesCount(count),
                                Set(ps(nodeD, ReachableNodesCount.Key)),
                                c(nodeB)
                            )
                    }
                }

                def lazyAnalysis(n: Node): ProperPropertyComputationResult = n match {
                    case `nodeA` =>
                        val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                        InterimResult(
                            nodeA, ReachableNodesCount(20), ReachableNodesCount(0),
                            Set(bEOptionP),
                            c(nodeA)
                        )

                    case `nodeB` =>
                        val cEOptionP = ps(nodeC, ReachableNodesCount.Key)
                        count += 1
                        InterimResult(
                            n, ReachableNodesCount(100 - count), ReachableNodesCount(count),
                            Set(cEOptionP),
                            c(nodeB)
                        )

                    case `nodeC` =>
                        new Result(nodeC, ReachableNodesCount(0))

                    case `nodeD` =>
                        new Result(nodeD, ReachableNodesCount(0))
                }

                ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
                try {
                    ps.force(nodeA, ReachableNodesCount.Key)
                    ps.waitOnPhaseCompletion()
                    fail("invalid update not detected")
                } catch {
                    case iae: IllegalArgumentException if iae.getMessage.contains("illegal update") =>
                    // OK - EXPECTED
                    case e: Throwable =>
                        e.printStackTrace()
                        fail(e.getMessage)
                }
            }

            afterAll(ps)
            ps.shutdown()
        }

        it("should catch non-monotonic updates related to the upper bound") {
            val ps = createPropertyStore()
            if (ps.MaxEvaluationDepth == 0) {

                assert(PropertyStore.Debug, "debugging is turned off")

                var count = 0
                val nodeA = Node("a")
                val nodeB = Node("b")
                val nodeC = Node("c")
                val nodeD = Node("d")

                info(
                    s"PropertyStore(MaxEvaluationDepth=${ps.MaxEvaluationDepth})"+
                        s"@${System.identityHashCode(ps).toHexString}"
                )

                ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                def c(n: Node)(eOptP: SomeEOptionP): ProperPropertyComputationResult = {
                    n match {
                        case `nodeA` =>
                            Result(nodeA, ReachableNodesCount(50))
                        case `nodeB` =>
                            InterimResult(
                                n,
                                ReachableNodesCount(100 - count),
                                ReachableNodesCount(0), // <= invalid refinement of upper bound!
                                Set(ps(nodeD, ReachableNodesCount.Key)),
                                c(nodeB)
                            )
                    }
                }

                def lazyAnalysis(n: Node): ProperPropertyComputationResult = n match {
                    case `nodeA` =>
                        val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                        InterimResult(
                            nodeA, ReachableNodesCount(20), ReachableNodesCount(0),
                            Set(bEOptionP),
                            c(nodeA)
                        )

                    case `nodeB` =>
                        val cEOptionP = ps(nodeC, ReachableNodesCount.Key)
                        count += 1
                        InterimResult(
                            n, ReachableNodesCount(100 - count), ReachableNodesCount(10),
                            Set(cEOptionP),
                            c(nodeB)
                        )

                    case `nodeC` => new Result(nodeC, ReachableNodesCount(0))

                    case `nodeD` => new Result(nodeD, ReachableNodesCount(0))
                }

                ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
                try {
                    ps.force(nodeA, ReachableNodesCount.Key)
                    ps.waitOnPhaseCompletion()
                    fail("invalid update not detected")
                } catch {
                    case iae: IllegalArgumentException if iae.getMessage.contains("illegal update") =>
                    // OK - EXPECTED
                    case e: Throwable =>
                        e.printStackTrace()
                        fail(e.getMessage)
                }
            }

            afterAll(ps)
            ps.shutdown()
        }

        {
            val ps = createPropertyStore()
            if (ps.MaxEvaluationDepth == 0) {
                it("should catch updates when the upper bound is lower than the lower bound") {
                    assert(PropertyStore.Debug, "debugging is turned off")
                    var count = 0
                    val nodeA = Node("a")
                    val nodeB = Node("b")
                    val nodeC = Node("c")
                    val nodeD = Node("d")

                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                    def c(n: Node)(eOptP: SomeEOptionP): ProperPropertyComputationResult = {
                        n match {
                            case `nodeA` =>
                                new Result(nodeA, ReachableNodesCount(50))
                            case `nodeB` =>
                                InterimResult(
                                    n,
                                    ReachableNodesCount(40),
                                    ReachableNodesCount(50),
                                    Set(ps(nodeD, ReachableNodesCount.Key)),
                                    c(nodeB)
                                )
                        }
                    }

                    def lazyAnalysis(n: Node): ProperPropertyComputationResult = n match {
                        case `nodeA` =>
                            val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                            InterimResult(
                                nodeA, ReachableNodesCount(20), ReachableNodesCount(0),
                                Set(bEOptionP),
                                c(nodeA)
                            )

                        case `nodeB` =>
                            val cEOptionP = ps(nodeC, ReachableNodesCount.Key)
                            count += 1
                            InterimResult(
                                n, ReachableNodesCount(100 - count), ReachableNodesCount(10),
                                Set(cEOptionP),
                                c(nodeB)
                            )

                        case `nodeC` => new Result(nodeC, ReachableNodesCount(0))

                        case `nodeD` => new Result(nodeD, ReachableNodesCount(0))
                    }

                    ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
                    try {
                        ps.force(nodeA, ReachableNodesCount.Key)
                        ps.waitOnPhaseCompletion()
                        fail("invalid update not detected")
                    } catch {
                        case iae: IllegalArgumentException if iae.getMessage.contains("is not equal or better than") =>
                        // OK - EXPECTED
                        case e: Throwable =>
                            e.printStackTrace()
                            fail(e.getMessage)
                    }

                }
            }

            afterAll(ps)
            ps.shutdown()
        }
    }

}

abstract class PropertyStoreTestWithoutDebugging[PS <: PropertyStore] extends PropertyStoreTest[PS] {

    private[this] val oldPropertyStoreUpdateSetting = PropertyStore.Debug
    override def beforeAll(): Unit = PropertyStore.updateDebug(false)
    override def afterAll(): Unit = PropertyStore.updateDebug(oldPropertyStoreUpdateSetting)

}
