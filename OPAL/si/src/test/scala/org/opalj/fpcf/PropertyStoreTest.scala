/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.util.concurrent.atomic.AtomicInteger

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.fpcf.fixtures._

/**
 * Tests the property store.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
sealed abstract class PropertyStoreTest(
        val propertyComputationHints: Seq[PropertyComputationHint]
) extends FunSpec with Matchers with BeforeAndAfterAll {

    implicit val logContext: LogContext = GlobalLogContext

    def createPropertyStore(): PropertyStore

    propertyComputationHints foreach { pch ⇒
        describe(s"using a PropertyStore (property computations use the hint=$pch)") {

            import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
            import org.opalj.fpcf.fixtures.Palindromes.NoSuperPalindrome
            import org.opalj.fpcf.fixtures.Palindromes.Palindrome
            import org.opalj.fpcf.fixtures.Palindromes.PalindromeKey
            import org.opalj.fpcf.fixtures.Palindromes.PalindromePropertyNotAnalyzed
            import org.opalj.fpcf.fixtures.Palindromes.SuperPalindrome
            import org.opalj.fpcf.fixtures.Palindromes.SuperPalindromeKey

            it("should be possible to query an empty property") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.quiescenceCount should be(0)
                ps.scheduledTasksCount should be(0)
                ps.fastTrackPropertiesCount should be(0)
                ps.scheduledOnUpdateComputationsCount should be(0)
                ps.immediateOnUpdateComputationsCount should be(0)
                ps.isKnown("<DOES NOT EXIST>") should be(false)
                ps.hasProperty("<DOES NOT EXIST>", Palindrome) should be(false)
                ps.properties("<DOES NOT EXIST>") should be('Empty)
                ps.entities(_ ⇒ true) should be('Empty)
                ps.entities(Palindrome, Palindrome) should be('Empty)
                ps.entities(PalindromeKey) should be('Empty)
                ps.finalEntities(Palindrome) should be('Empty)

                ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

                ps.isKnown("<DOES NOT EXIST>") should be(false)
                ps.hasProperty("<DOES NOT EXIST>", Palindrome) should be(false)
                ps.properties("<DOES NOT EXIST>") should be('Empty)
                ps.entities(_ ⇒ true) should be('Empty)
                ps.entities(Palindrome, Palindrome) should be('Empty)
                ps.entities(PalindromeKey) should be('Empty)
                ps.finalEntities(Palindrome) should be('Empty)

                ps.waitOnPhaseCompletion()

                ps.quiescenceCount should be(1)
                ps.scheduledTasksCount should be(0)
                ps.fastTrackPropertiesCount should be(0)
                ps.scheduledOnUpdateComputationsCount should be(0)
                ps.immediateOnUpdateComputationsCount should be(0)
                ps.isKnown("<DOES NOT EXIST>") should be(false)
                ps.hasProperty("<DOES NOT EXIST>", Palindrome) should be(false)
                ps.properties("<DOES NOT EXIST>") should be('Empty)
                ps.entities(_ ⇒ true) should be('Empty)
                ps.entities(Palindrome, Palindrome) should be('Empty)
                ps.entities(PalindromeKey) should be('Empty)
                ps.finalEntities(Palindrome) should be('Empty)

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

                ps.shutdown()
            }

            it("should be possible to terminate the computations and still query the store") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(PalindromeKey), Set.empty)
                ps.scheduleEagerComputationForEntity("a") { e ⇒
                    ps.doTerminate = true
                    val dependee = EPK("d", Palindromes.PalindromeKey)
                    ps(dependee) // we use a fake dependency...
                    InterimResult(
                        "a",
                        NoPalindrome,
                        Palindrome,
                        Seq(dependee),
                        eps ⇒ { Result("a", Palindrome) },
                        pch
                    )
                }
                assertThrows[InterruptedException] { ps.waitOnPhaseCompletion() }

                ps("a", PalindromeKey) should be(InterimELUBP("a", NoPalindrome, Palindrome))

                ps.shutdown()
            }

            it("should not crash when e1 has two dependencies e2 and e3 "+
                "and e2 is set while e1 was not yet executed but had a EPK for e2 in its dependencies "+
                "(test for a lost updated)") {

                import org.opalj.fpcf.fixtures.Palindromes.PalindromeKey
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.set("e2", Palindrome)

                ps.setupPhase(Set(PalindromeKey), Set.empty)

                ps.scheduleEagerComputationForEntity("e1") { e ⇒
                    val dependees = Seq(EPK("e2", PalindromeKey), EPK("e3", PalindromeKey))
                    dependees.foreach(ps.apply[Entity, Property]) // we have to quey them!
                    InterimResult(
                        "e1",
                        NoPalindrome,
                        Palindrome,
                        dependees,
                        eps ⇒ { Result("e1", Palindrome) },
                        pch
                    )
                }
                ps.waitOnPhaseCompletion()

                ps("e1", PalindromeKey) should be(FinalEP("e1", Palindrome))
                ps("e2", PalindromeKey) should be(FinalEP("e2", Palindrome))
                ps("e3", PalindromeKey) should be(FinalEP("e3", PalindromePropertyNotAnalyzed))

                ps.shutdown()
            }

            it("should be able to perform queries w.r.t. unknown entities / property keys") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(PalindromeKey), Set.empty)
                ps("aba", PalindromeKey) should be(EPK("aba", PalindromeKey))
                ps(EPK("aa", PalindromeKey)) should be(EPK("aa", PalindromeKey))

                ps.shutdown()
            }

            it("should be possible to test if a store has a property") {
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

                ps.hasProperty("aba", PalindromeKey) should be(true)
                ps.hasProperty("zzYzz", SuperPalindromeKey) should be(true)

                ps.hasProperty("cbc", PalindromeKey) should be(false)
                ps.hasProperty("aba", SuperPalindromeKey) should be(false)

                ps.scheduleEagerComputationForEntity("a") { e ⇒
                    val dependee = EPK("d", PalindromeKey)
                    ps(dependee)
                    InterimResult(
                        "a",
                        NoPalindrome, Palindrome,
                        Seq(dependee),
                        _ ⇒ Result("a", Palindrome),
                        pch
                    )
                }
                ps.waitOnPhaseCompletion()

                ps.hasProperty("a", PalindromeKey) should be(true)
                ps.hasProperty("d", PalindromeKey) should be(true)

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

                ps.shutdown()
            }

            it("should specify that no analysis was scheduled when requesting the fallback in the respective case") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                // NOT import Palindromes.PalindromePropertyNotAnalyzed
                import org.opalj.fpcf.fixtures.Palindromes.NoAnalysisForPalindromeProperty
                ps.setupPhase(Set(Marker.MarkerKey), Set.empty)

                val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")

                ps.scheduleEagerComputationsForEntities[String](es) { e ⇒
                    def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                        eps match {
                            case FinalP(NoAnalysisForPalindromeProperty) /*<= the test...*/ ⇒
                                Result(e, Marker.NotMarked)
                            case epk: SomeEPK ⇒
                                InterimResult(e, Marker.IsMarked, Marker.NotMarked, List(epk), c)
                        }
                    }
                    c(ps(e, Palindromes.PalindromeKey))
                }
                ps.waitOnPhaseCompletion()

                ps.entities(Marker.NotMarked, Marker.NotMarked).toSet should be(es)

                ps.shutdown()
            }

            it("should specify that the property was not derived when an analysis was scheduled") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.Palindrome
                import org.opalj.fpcf.fixtures.Palindromes.PalindromePropertyNotAnalyzed
                // import Palindromes.NoAnalysisForPalindromeProperty
                ps.setupPhase(Set(Palindromes.PalindromeKey, Marker.MarkerKey), Set.empty)

                val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
                ps.scheduleEagerComputationsForEntities(Set("aba", "cc", "d")) { e ⇒
                    Result(e, if (e.reverse == e) Palindrome else NoPalindrome)
                }
                ps.scheduleEagerComputationsForEntities(es) { e ⇒
                    def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                        eps match {
                            case FinalP(Palindrome) | FinalP(NoPalindrome) ⇒
                                Result(e, Marker.IsMarked)

                            case FinalP(PalindromePropertyNotAnalyzed) ⇒
                                Result(e, Marker.NotMarked)

                            case epk: SomeEPK ⇒ InterimResult(
                                e, Marker.IsMarked, Marker.NotMarked, List(epk), c
                            )
                        }
                    }
                    c(ps(e, Palindromes.PalindromeKey))
                }
                ps.waitOnPhaseCompletion()

                ps.entities(Marker.NotMarked, Marker.NotMarked).toSet should be(Set("fd", "zu", "aaabbbaaa"))

                ps.shutdown()
            }

            it("should not set an entity's property if it already has a property") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.Palindrome

                ps.set("aba", Palindrome)
                assertThrows[IllegalStateException] { ps.set("aba", NoPalindrome) }

                ps.shutdown()
            }

            it("should contain the results (at least) for all entities for which we scheduled a computation") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.Palindrome
                ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

                val pk = Palindromes.PalindromeKey
                val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
                ps.scheduleEagerComputationsForEntities(es) { e ⇒
                    Result(e, if (e.reverse == e) Palindrome else NoPalindrome)
                }
                ps.waitOnPhaseCompletion()

                ps.entities(pk).map(_.e).toSet should be(es)
                ps.entities(eps ⇒ eps.lb == Palindrome).toSet should be(Set("aba", "cc", "d", "aaabbbaaa"))
                ps.entities(eps ⇒ eps.ub == NoPalindrome).toSet should be(Set("fd", "zu"))
                ps.entities(Palindrome, Palindrome).toSet should be(Set("aba", "cc", "d", "aaabbbaaa"))
                ps.entities(NoPalindrome, NoPalindrome).toSet should be(Set("fd", "zu"))
                ps.entities(pk).toSet should be(Set(
                    FinalEP("aba", Palindrome),
                    FinalEP("cc", Palindrome),
                    FinalEP("d", Palindrome),
                    FinalEP("fd", NoPalindrome),
                    FinalEP("zu", NoPalindrome),
                    FinalEP("aaabbbaaa", Palindrome)
                ))

                es.foreach { e ⇒
                    val expected = if (e.reverse == e) Palindrome else NoPalindrome
                    ps.properties(e).map(_.ub).toSet should be(Set(expected))
                    ps.properties(e).map(_.lb).toSet should be(Set(expected))
                }

                ps.shutdown()
            }

            it("should trigger a lazy property computation only lazily") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.Palindrome
                val pk = Palindromes.PalindromeKey
                ps.setupPhase(Set(pk), Set.empty)
                ps.registerLazyPropertyComputation(
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
                ps("aba", pk) should be(FinalEP("aba", Palindrome))

                ps.shutdown()
            }

            it("should not trigger a lazy property computation multiple times") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.Palindrome
                val pk = Palindromes.PalindromeKey
                ps.setupPhase(Set(pk), Set.empty)

                val invocationCount = new AtomicInteger(0)
                ps.registerLazyPropertyComputation(
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

                if (invocationCount.get != 1) {
                    fail(s"invocation count should be 1; was ${invocationCount.get}")
                }

                ps.shutdown()
            }

            it("should complete the computation of dependent lazy computations before the phase ends") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                import org.opalj.fpcf.fixtures.Palindromes.NoPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.NoSuperPalindrome
                import org.opalj.fpcf.fixtures.Palindromes.Palindrome
                import org.opalj.fpcf.fixtures.Palindromes.SuperPalindrome
                val ppk = Palindromes.PalindromeKey
                val sppk = Palindromes.SuperPalindromeKey

                // let's see if we can register some values...
                ps.set("dummyymmud", Palindrome)
                ps.set("dummyBADymmud", NoPalindrome)
                ps.set("dummyymmud", SuperPalindrome)
                ps.set("dummyBADymmud", NoSuperPalindrome)

                ps.setupPhase(Set(ppk, sppk), Set.empty)

                val invocationCount = new AtomicInteger(0)
                ps.registerLazyPropertyComputation(
                    ppk,
                    (e: Entity) ⇒ {
                        invocationCount.incrementAndGet()
                        val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                        Result(e, p)
                    }
                )
                ps.registerLazyPropertyComputation(
                    sppk,
                    (e: Entity) ⇒ {
                        invocationCount.incrementAndGet()

                        val initialsExpectedEP = EPK(e, ppk)
                        ps(e, ppk) should be(initialsExpectedEP)

                        InterimResult(
                            e, NoSuperPalindrome, SuperPalindrome,
                            Seq(initialsExpectedEP),
                            (eps) ⇒ {
                                if (eps.lb == Palindrome /*&& ...*/ )
                                    Result(e, SuperPalindrome)
                                else
                                    Result(e, NoSuperPalindrome)
                            },
                            pch
                        )
                    }
                )
                ps.scheduleEagerComputationForEntity("e") { e: String ⇒
                    val initiallyExpectedEP = EPK("e", sppk)
                    ps("e", sppk) should be(initiallyExpectedEP)
                    InterimResult(
                        "e", Marker.NotMarked, Marker.IsMarked,
                        Seq(initiallyExpectedEP),
                        eps ⇒ {
                            // Depending the scheduling, we can have a final result here as well.
                            if (eps.isFinal) {
                                if (eps.lb == SuperPalindrome)
                                    Result(e, Marker.IsMarked)
                                else
                                    Result(e, Marker.NotMarked)
                            } else
                                InterimResult(
                                    "e", Marker.NotMarked, Marker.IsMarked,
                                    Seq(eps),
                                    (eps) ⇒ {
                                        if (!eps.isFinal)
                                            fail("unexpected non final value")

                                        if (eps.lb == SuperPalindrome)
                                            Result(e, Marker.IsMarked)
                                        else
                                            Result(e, Marker.NotMarked)
                                    },
                                    pch
                                )
                        }
                    )
                }
                ps.waitOnPhaseCompletion()

                ps("e", ppk) should be(FinalEP("e", Palindrome))
                ps("e", sppk) should be(FinalEP("e", SuperPalindrome))
                ps("e", Marker.MarkerKey) should be(FinalEP("e", Marker.IsMarked))

                ps("dummyymmud", ppk) should be(FinalEP("dummyymmud", Palindrome))
                ps("dummyBADymmud", ppk) should be(FinalEP("dummyBADymmud", NoPalindrome))

                ps("dummyymmud", sppk) should be(FinalEP("dummyymmud", SuperPalindrome))
                ps("dummyBADymmud", sppk) should be(FinalEP("dummyBADymmud", NoSuperPalindrome))

                ps.shutdown()
            }

            describe("support for fast track properties") {

                it("should correctly handle lazy computations that support fast track properties") {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    if (ps.supportsFastTrackPropertyComputations) {

                        ps.useFastTrackPropertyComputations = true
                        assert(ps.useFastTrackPropertyComputations)

                        import MarkerWithFastTrack._
                        ps.setupPhase(Set(MarkerWithFastTrackKey), Set.empty)

                        val entities = (1 to 50).map(i ⇒ "e"+i)
                        // basically, we should ALWAYS get the result of the fast-track computation..
                        ps.registerLazyPropertyComputation(
                            MarkerWithFastTrackKey,
                            (e: Entity) ⇒ {
                                entities.filter(_ != e).foreach(ps.apply(_, MarkerWithFastTrackKey))
                                Result(e, NotMarked)
                            }
                        )
                        entities foreach { ps.force(_, MarkerWithFastTrackKey) }
                        ps.waitOnPhaseCompletion()
                        entities foreach { e ⇒
                            ps(e, MarkerWithFastTrackKey) should be(FinalEP(e, IsMarked))
                        }
                        MarkerWithFastTrack.fastTrackEvaluationsCount(ps) should be(50)
                    }

                    ps.shutdown()
                }
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
                val nodeEntities = List[Node](
                    nodeA, nodeB, nodeC, nodeD, nodeE, nodeF, nodeG, nodeH, nodeI, nodeJ, nodeR
                )

                object ReachableNodes {
                    val Key: PropertyKey[ReachableNodes] =

                        PropertyKey.create[Node, ReachableNodes](
                            s"ReachableNodes(t=${System.nanoTime()})",
                            (_: PropertyStore, reason: FallbackReason, e: Node) ⇒ AllNodes,
                            (ps: PropertyStore, e: Entity) ⇒ None
                        )
                }
                case class ReachableNodes(nodes: scala.collection.Set[Node]) extends OrderedProperty {
                    type Self = ReachableNodes

                    def key = ReachableNodes.Key

                    def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
                        if (!this.nodes.subsetOf(other.nodes)) {
                            throw new IllegalArgumentException(
                                s"$e: $this is not equal or better than $other"
                            )
                        }
                    }
                }
                object NoReachableNodes extends ReachableNodes(Set.empty) {
                    override def toString: String = "NoReachableNodes"
                }
                object AllNodes extends ReachableNodes(nodeEntities.toSet) {
                    override def toString: String = "AllNodes"
                }

                /*
             * The following analysis only uses the new information given to it and updates
             * the set of observed dependees.
             */
                def reachableNodesAnalysis(
                    ps: PropertyStore
                )(
                    n: Node
                ): ProperPropertyComputationResult = {
                    val nTargets = n.targets
                    if (nTargets.isEmpty)
                        return Result(n, NoReachableNodes);

                    var allDependees: Set[Node] = nTargets.toSet // may include self-dependency
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
                                InterimResult(n, AllNodes, newUB, dependeePs, c, pch)
                            else
                                Result(n, newUB)
                        }
                        r
                    }

                    // initial computation
                    dependeePs foreach { dependeeP ⇒
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
                        InterimResult(n, AllNodes, currentReachableNodes, dependeePs, c, pch)
                }

                def reachableNodesCountAnalysis(ps: PropertyStore)(
                    n: Node
                ): ProperPropertyComputationResult = {
                    var dependees: List[SomeEOptionP] = Nil
                    var ub: Int = n.targets.size

                    def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                        eps match {
                            case InterimUBP(ReachableNodesCount(otherUB)) ⇒
                                if (ub + otherUB > 4)
                                    Result(n, TooManyNodesReachable)
                                else {
                                    dependees = eps :: dependees.filter(_.e ne eps.e)
                                    InterimResult(
                                        n, TooManyNodesReachable, ReachableNodesCount(ub),
                                        dependees,
                                        c,
                                        pch
                                    )
                                }

                            case FinalP(reachableNodesCount: ReachableNodesCount) ⇒
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
                                        c,
                                        pch
                                    )
                                }
                        }
                    }

                    n.targets forall { successor ⇒
                        if (successor == n) {
                            ub = TooManyNodesReachable.value
                            false // we are done...
                        } else {
                            ps(successor, ReachableNodesCount.Key) match {
                                case epk: EPK[_, _] ⇒
                                    dependees ::= epk
                                    true
                                case iep @ InterimUBP(ReachableNodesCount(otherUB)) ⇒
                                    if (ub + otherUB > 4) {
                                        ub = TooManyNodesReachable.value
                                        false
                                    } else {
                                        // we have to wait for the final value before we can add the count
                                        dependees ::= iep
                                        true
                                    }
                                case FinalP(reachableNodesCount) ⇒
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
                            n, TooManyNodesReachable, ReachableNodesCount(ub),
                            dependees,
                            c, pch
                        )
                }

                def reachableNodesCountViaReachableNodesAnalysis(ps: PropertyStore)(
                    n: Node
                ): ProperPropertyComputationResult = {

                    def c(eps: SomeEOptionP): ProperPropertyComputationResult = {
                        eps match {
                            case eps @ InterimUBP(ReachableNodes(reachableNodes)) ⇒
                                InterimResult(
                                    n, TooManyNodesReachable, ReachableNodesCount(reachableNodes.size),
                                    List(eps),
                                    c, pch
                                )

                            case FinalP(ReachableNodes(reachableNodes)) ⇒
                                Result(n, ReachableNodesCount(reachableNodes.size))
                        }
                    }

                    ps(n, ReachableNodes.Key) match {
                        case epk: EPK[_, _] ⇒
                            InterimResult(
                                n, TooManyNodesReachable, ReachableNodesCount(0),
                                List(epk),
                                c, pch
                            )
                        case eps: SomeEOptionP ⇒ c(eps)

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
                    val dropCount = (System.nanoTime() % 10000).toInt
                    var count = -1
                    for (nodeEntitiesPermutation ← nodeEntities.permutations.drop(dropCount).take(1000)) {
                        count += 1
                        if (count % 100 == 0) {
                            val ps = createPropertyStore()
                            info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                            ps.setupPhase(Set(ReachableNodesCount.Key, ReachableNodes.Key), Set.empty)
                            ps.registerLazyPropertyComputation(
                                ReachableNodesCount.Key, reachableNodesCountAnalysis(ps)
                            )
                            ps.scheduleEagerComputationsForEntities(nodeEntitiesPermutation)(reachableNodesAnalysis(ps))
                            ps(nodeA, ReachableNodesCount.Key) // forces the evaluation for all nodes...

                            ps.waitOnPhaseCompletion()

                            info(
                                s"(id of first permutation = ${dropCount + 1}; this permutation=${nodeEntitiesPermutation.mkString("[", ",", "|")} number of executed tasks:"+ps.scheduledTasksCount+
                                    "; number of scheduled onUpdateContinuations:"+ps.scheduledOnUpdateComputationsCount+
                                    "; number of immediate onUpdateContinuations:"+ps.immediateOnUpdateComputationsCount
                            )
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
                            } catch {
                                case t: Throwable ⇒
                                    throw t;
                            }

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
                        ReachableNodesCount.Key, reachableNodesCountViaReachableNodesAnalysis(ps)
                    )
                    nodeEntities foreach { node ⇒ ps.force(node, ReachableNodesCount.Key) }
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
                            FinalEP(nodeA, ReachableNodesCount(nodeEntities.toSet.size - 1))
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
                        case t: Throwable ⇒
                            throw t;
                    }

                    ps.shutdown()
                }

                it("should be possible to use lazily scheduled mutually dependent computations without itermediate results") {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(
                        Set(ReachableNodes.Key, ReachableNodesCount.Key),
                        Set.empty,
                        Map(ReachableNodesCount.Key → Set(ReachableNodes.Key))
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodes.Key, reachableNodesAnalysis(ps)
                    )
                    ps.registerLazyPropertyComputation(
                        ReachableNodesCount.Key, reachableNodesCountViaReachableNodesAnalysis(ps)
                    )
                    nodeEntities foreach { node ⇒ ps.force(node, ReachableNodesCount.Key) }
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
                            FinalEP(nodeA, ReachableNodesCount(nodeEntities.toSet.size - 1))
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
                        case t: Throwable ⇒
                            throw t;
                    }

                    ps.shutdown()
                }

                it("should be possible when a lazy computation depends on properties for which no analysis is scheduled") {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)
                    // WE DO NOT SCHEDULE ReachableNodes
                    ps.registerLazyPropertyComputation(
                        ReachableNodesCount.Key, reachableNodesCountViaReachableNodesAnalysis(ps)
                    )
                    nodeEntities.foreach { node ⇒ ps(node, ReachableNodesCount.Key) }
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

                    ps.shutdown()
                }

                it("should be possible when a lazy computation depends on properties for which analysis seems to be scheduled, but no analysis actually produces results") {
                    val ps = createPropertyStore()
                    info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                    ps.setupPhase(Set(ReachableNodesCount.Key, ReachableNodes.Key), Set.empty)
                    // WE DO NOT SCHEDULE ReachableNodes
                    ps.registerLazyPropertyComputation(
                        ReachableNodesCount.Key, reachableNodesCountViaReachableNodesAnalysis(ps)
                    )
                    nodeEntities.foreach { node ⇒ ps(node, ReachableNodesCount.Key) }
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

                    ps.shutdown()
                }

            }

            it("should be possible to execute an analysis incrementally") {
                import scala.collection.mutable

                class Node(val name: String, val targets: mutable.Set[Node] = mutable.Set.empty) {
                    override def hashCode: Int = name.hashCode()

                    override def equals(other: Any): Boolean = other match {
                        case that: Node ⇒ this.name equals that.name
                        case _          ⇒ false
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
                        (ps: PropertyStore, reason: FallbackReason, e: Entity) ⇒ ???,
                        (ps: PropertyStore, e: Entity) ⇒ None
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
                    val nextPCs = n.targets.map(t ⇒ (analysis(level + 1) _, t)).iterator
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

                ps.shutdown()
            }

            it("should never pass a null property to clients") {
                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                val ppk = Palindromes.PalindromeKey
                ps.setupPhase(Set(ppk), Set.empty)

                ps.registerLazyPropertyComputation(
                    ppk,
                    (e: Entity) ⇒ {
                        val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                        Result(e, p)
                    }
                )
                ps.scheduleEagerComputationForEntity("aaa") { s: String ⇒
                    ps("a", ppk) match {
                        case epk: EPK[_, _] ⇒
                            InterimResult(
                                s, NoPalindrome, Palindrome,
                                List(epk),
                                (eps: SomeEPS) ⇒ {
                                    if (eps.lb == null || eps.ub == null)
                                        fail("clients should never see null properties")
                                    else
                                        Result(s, Palindrome)
                                },
                                pch
                            )
                        case _ ⇒ fail("unexpected result")
                    }
                }

                ps.shutdown()
            }

            it("should be possible to execute an analysis which analyzes a huge circle") {
                import scala.collection.mutable

                val testSizes = Set(1, 5, 50000)
                for (testSize ← testSizes) {
                    // 1. we create a ((very) long) chain
                    val firstNode = Node(0.toString)
                    val allNodes = mutable.Set(firstNode)
                    var prevNode = firstNode
                    for { i ← 1 to testSize } {
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
                            // HERE - For this test case only, we can simple get to the previous
                            // node from the one that was updated.
                            successorNode match {

                                case epk: EPK[_, _] ⇒
                                    InterimResult(node, Impure, Pure, Iterable(epk), c, pch)

                                case eps @ InterimLUBP(lb: Property, ub: Property) ⇒
                                    InterimResult(node, lb, ub, Iterable(eps), c, pch)

                                // required when we resolve the cycle
                                case FinalP(Pure)       ⇒ Result(node, Pure)

                                // the following cases should never happen...
                                case InterimLBP(Impure) ⇒ ???
                                case FinalP(Impure)     ⇒ ???
                            }
                        }

                        val nextNode = node.targets.head // HERE: we always have only one successor
                        c(ps(nextNode, Purity.Key))
                    }
                    // 4. execute analysis
                    ps.scheduleEagerComputationsForEntities(allNodes)(purityAnalysis)
                    ps.waitOnPhaseCompletion()

                    // 5. let's evaluate the result
                    ps.entities(Purity.Key) foreach { eps ⇒
                        if (eps.lb != Pure) {
                            info(ps.toString(true))
                            fail(s"Node(${eps.e}) is not Pure (${eps.lb})")
                        }
                    }

                    ps.shutdown()

                    info(s"test succeeded with $testSize node(s) in a circle")
                    info(
                        s"number of executed tasks:"+ps.scheduledTasksCount+
                            "; number of scheduled onUpdateContinuations:"+ps.scheduledOnUpdateComputationsCount+
                            "; number of immediate onUpdateContinuations:"+ps.immediateOnUpdateComputationsCount
                    )
                }
            }
        }
    }
}

abstract class PropertyStoreTestWithDebugging(
        propertyComputationHints: Seq[PropertyComputationHint] = List(CheapPropertyComputation, DefaultPropertyComputation)
) extends PropertyStoreTest(propertyComputationHints) {

    private[this] var oldPropertyStoreUpdateSetting = PropertyStore.Debug
    override def beforeAll(): Unit = PropertyStore.updateDebug(true)
    override def afterAll(): Unit = PropertyStore.updateDebug(oldPropertyStoreUpdateSetting)

    propertyComputationHints foreach { pch ⇒

        describe(s"the property store with debugging support and $pch") {

            it("should catch InterimResults with inverted property bounds") {
                assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

                val nodeA = Node("a")
                val nodeB = Node("b")

                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                def aAnalysis(ignored: Node): PropertyComputationResult = {
                    val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                    InterimResult(
                        nodeA, ReachableNodesCount(10), ReachableNodesCount(20), List(bEOptionP),
                        (bStringEOptionP: SomeEOptionP) ⇒ ???,
                        pch
                    )
                }

                try {
                    ps.scheduleEagerComputationForEntity(nodeA)(aAnalysis)
                    ps.waitOnPhaseCompletion()
                    fail("invalid update not detected")
                } catch {
                    case iae: IllegalArgumentException if (
                        iae.getMessage.contains("is not equal or better than")
                    ) ⇒ // OK - EXPECTED
                }

                ps.shutdown()
            }

            it("should catch non-monotonic updates related to the lower bound") {
                assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

                var count = 0
                val nodeA = Node("a")
                val nodeB = Node("b")
                val nodeC = Node("c")
                val nodeD = Node("d")

                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                def c(n: Node)(eOptP: SomeEOptionP): ProperPropertyComputationResult = {
                    n match {
                        case `nodeA` ⇒
                            Result(nodeA, ReachableNodesCount(50))
                        case `nodeB` ⇒
                            InterimResult(
                                n,
                                ReachableNodesCount(100), // <= invalid refinement of lower bound!
                                ReachableNodesCount(count),
                                List(ps(nodeD, ReachableNodesCount.Key)),
                                c(nodeB), pch
                            )
                    }
                }

                def lazyAnalysis(n: Node): ProperPropertyComputationResult = n match {
                    case `nodeA` ⇒
                        val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                        InterimResult(
                            nodeA, ReachableNodesCount(20), ReachableNodesCount(0),
                            List(bEOptionP),
                            c(nodeA), pch
                        )

                    case `nodeB` ⇒
                        val cEOptionP = ps(nodeC, ReachableNodesCount.Key)
                        count += 1
                        InterimResult(
                            n, ReachableNodesCount(100 - count), ReachableNodesCount(count),
                            List(cEOptionP),
                            c(nodeB), pch
                        )

                    case `nodeC` ⇒
                        new Result(nodeC, ReachableNodesCount(0))

                    case `nodeD` ⇒
                        new Result(nodeD, ReachableNodesCount(0))
                }

                ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
                try {
                    ps.force(nodeA, ReachableNodesCount.Key)
                    ps.waitOnPhaseCompletion()
                    fail("invalid update not detected")
                } catch {
                    case iae: IllegalArgumentException if iae.getMessage.contains("illegal update") ⇒
                    // OK - EXPECTED
                    case e: Throwable ⇒
                        e.printStackTrace()
                        fail(e.getMessage)
                }

                ps.shutdown()
            }

            it("should catch non-monotonic updates related to the upper bound") {
                assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

                var count = 0
                val nodeA = Node("a")
                val nodeB = Node("b")
                val nodeC = Node("c")
                val nodeD = Node("d")

                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                def c(n: Node)(eOptP: SomeEOptionP): ProperPropertyComputationResult = {
                    n match {
                        case `nodeA` ⇒
                            Result(nodeA, ReachableNodesCount(50))
                        case `nodeB` ⇒
                            InterimResult(
                                n,
                                ReachableNodesCount(100 - count),
                                ReachableNodesCount(0), // <= invalid refinement of upper bound!
                                List(ps(nodeD, ReachableNodesCount.Key)),
                                c(nodeB), pch
                            )
                    }
                }

                def lazyAnalysis(n: Node): ProperPropertyComputationResult = n match {
                    case `nodeA` ⇒
                        val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                        InterimResult(
                            nodeA, ReachableNodesCount(20), ReachableNodesCount(0),
                            List(bEOptionP),
                            c(nodeA), pch
                        )

                    case `nodeB` ⇒
                        val cEOptionP = ps(nodeC, ReachableNodesCount.Key)
                        count += 1
                        InterimResult(
                            n, ReachableNodesCount(100 - count), ReachableNodesCount(10),
                            List(cEOptionP),
                            c(nodeB), pch
                        )

                    case `nodeC` ⇒
                        new Result(nodeC, ReachableNodesCount(0))

                    case `nodeD` ⇒
                        new Result(nodeD, ReachableNodesCount(0))
                }

                ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
                try {
                    ps.force(nodeA, ReachableNodesCount.Key)
                    ps.waitOnPhaseCompletion()
                    fail("invalid update not detected")
                } catch {
                    case iae: IllegalArgumentException if iae.getMessage.contains("illegal update") ⇒
                    // OK - EXPECTED
                    case e: Throwable ⇒
                        e.printStackTrace()
                        fail(e.getMessage)
                }

                ps.shutdown()
            }

            it("should catch updates when the upper bound is lower than the lower bound") {
                assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state
                var count = 0
                val nodeA = Node("a")
                val nodeB = Node("b")
                val nodeC = Node("c")
                val nodeD = Node("d")

                val ps = createPropertyStore()
                info(s"PropertyStore@${System.identityHashCode(ps).toHexString}")

                ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

                def c(n: Node)(eOptP: SomeEOptionP): ProperPropertyComputationResult = {
                    n match {
                        case `nodeA` ⇒
                            new Result(nodeA, ReachableNodesCount(50))
                        case `nodeB` ⇒
                            InterimResult(
                                n,
                                ReachableNodesCount(40),
                                ReachableNodesCount(50),
                                List(ps(nodeD, ReachableNodesCount.Key)),
                                c(nodeB), pch
                            )
                    }
                }

                def lazyAnalysis(n: Node): ProperPropertyComputationResult = n match {
                    case `nodeA` ⇒
                        val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                        InterimResult(
                            nodeA, ReachableNodesCount(20), ReachableNodesCount(0),
                            List(bEOptionP),
                            c(nodeA), pch
                        )

                    case `nodeB` ⇒
                        val cEOptionP = ps(nodeC, ReachableNodesCount.Key)
                        count += 1
                        InterimResult(
                            n, ReachableNodesCount(100 - count), ReachableNodesCount(10),
                            List(cEOptionP),
                            c(nodeB), pch
                        )

                    case `nodeC` ⇒
                        new Result(nodeC, ReachableNodesCount(0))

                    case `nodeD` ⇒
                        new Result(nodeD, ReachableNodesCount(0))
                }

                ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
                try {
                    ps.force(nodeA, ReachableNodesCount.Key)
                    ps.waitOnPhaseCompletion()
                    fail("invalid update not detected")
                } catch {
                    case iae: IllegalArgumentException if iae.getMessage.contains("is not equal or better than") ⇒
                    // OK - EXPECTED
                    case e: Throwable ⇒
                        e.printStackTrace()
                        fail(e.getMessage)
                }

                ps.shutdown()
            }
        }
    }
}

abstract class PropertyStoreTestWithoutDebugging(
        propertyComputationHints: Seq[PropertyComputationHint]
) extends PropertyStoreTest(propertyComputationHints) {

    private[this] var oldPropertyStoreUpdateSetting = PropertyStore.Debug
    override def beforeAll(): Unit = PropertyStore.updateDebug(false)
    override def afterAll(): Unit = PropertyStore.updateDebug(oldPropertyStoreUpdateSetting)

}

