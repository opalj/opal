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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterAll

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.log.GlobalLogContext

/**
 * Tests the property store.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
sealed abstract class PropertyStoreTest extends FunSpec with Matchers with BeforeAndAfterAll {

    import Palindromes.NoPalindrome
    import Palindromes.Palindrome

    implicit val logContext = GlobalLogContext

    def createPropertyStore(): PropertyStore

    describe("the property store") {

        it("directly after creation it should be empty (entities(...) and properties(...))") {
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)
            ps.entities(_ ⇒ true) should be('Empty)
            ps.entities(Palindromes.Palindrome, Palindromes.Palindrome) should be('Empty)
            ps.entities(Palindromes.NoPalindrome, Palindromes.Palindrome) should be('Empty)
            ps.entities(Palindromes.NoPalindrome, Palindromes.NoPalindrome) should be('Empty)
            ps.entities(Palindromes.PalindromeKey) should be('Empty)
            ps.properties("<DOES NOT EXIST>") should be('Empty)
            ps.toString(true).length should be > (0)

            ps.waitOnPhaseCompletion()
            ps.entities(_ ⇒ true) should be('Empty)
            ps.entities(Palindromes.Palindrome, Palindromes.Palindrome) should be('Empty)
            ps.entities(Palindromes.NoPalindrome, Palindromes.Palindrome) should be('Empty)
            ps.entities(Palindromes.NoPalindrome, Palindromes.NoPalindrome) should be('Empty)
            ps.entities(Palindromes.PalindromeKey) should be('Empty)
            ps.properties("<DOES NOT EXIST>") should be('Empty)
            ps.toString(true).length should be > (0)
        }

        it("should be possible to interrupt the computations") {
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

            ps.scheduleEagerComputationForEntity("a") { e ⇒
                ps.isSuspended = () ⇒ true
                val dependee = EPK("d", Palindromes.PalindromeKey)
                ps(dependee) // we use a fake dependency...
                IntermediateResult(
                    "a",
                    NoPalindrome,
                    Palindrome,
                    Seq(dependee),
                    (eps) ⇒ { Result("a", Palindrome) }
                )
            }
            ps.waitOnPhaseCompletion()
            // ps' "isInterrupt" status should now be true; hence, scheduling
            // further computations should have no effect.
            ps.scheduleEagerComputationForEntity("d")(e ⇒ Result("d", Palindrome))

            val aEP = ps("a", Palindromes.PalindromeKey)
            if (aEP != IntermediateEP("a", NoPalindrome, Palindrome) &&
                aEP != EPK("a", Palindromes.PalindromeKey)) {
                fail("the property store was not correctly suspended")
            }
            ps("d", Palindromes.PalindromeKey) should be(EPK("d", Palindromes.PalindromeKey))

            // let's test that – if we resume the computation – the results are as expected!
            ps.isSuspended = () ⇒ false
            ps.waitOnPhaseCompletion()
            ps("a", Palindromes.PalindromeKey) should be(FinalEP("a", Palindrome))
            ps("d", Palindromes.PalindromeKey) should be(FinalEP("d", Palindrome))
        }

        it("should not crash when e1 has two dependencies e2 and e3 "+
            "and e2 is set while e1 was not yet executed but had a EPK for e2 in its dependencies "+
            "(test for a lost updated)") {
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

            ps.scheduleEagerComputationForEntity("a") { e ⇒
                val dependees = Seq(EPK("d", Palindromes.PalindromeKey), EPK("e", Palindromes.PalindromeKey))
                dependees.foreach(ps(_)) // we use a fake dependency...
                ps.set("d", Palindrome)
                IntermediateResult(
                    "a",
                    NoPalindrome,
                    Palindrome,
                    dependees,
                    (eps) ⇒ { Result("a", Palindrome) }
                )
            }
            ps.waitOnPhaseCompletion()

            ps("a", Palindromes.PalindromeKey) should be(FinalEP("a", Palindrome))
            ps("d", Palindromes.PalindromeKey) should be(FinalEP("d", Palindrome))
            ps("e", Palindromes.PalindromeKey) should be(EPK("e", Palindromes.PalindromeKey))
        }

        it("should be able to perform queries w.r.t. unknown entities / property keys") {
            val pk = Palindromes.PalindromeKey
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

            ps("aba", pk) should be(EPK("aba", pk))
            ps(EPK("aa", pk)) should be(EPK("aa", pk))
        }

        it("should be possible to test if a store has a property") {
            import Palindromes.Palindrome
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindromes.PalindromeKey, Palindromes.SuperPalindromeKey), Set.empty)

            val palindromeKey = Palindromes.PalindromeKey
            val superPalindromeKey = Palindromes.SuperPalindromeKey

            ps.hasProperty("aba", palindromeKey) should be(false)
            ps.hasProperty("aba", superPalindromeKey) should be(false)

            ps.set("aba", Palindrome)
            ps.waitOnPhaseCompletion()
            ps.hasProperty("aba", palindromeKey) should be(true)
            ps.hasProperty("cbc", palindromeKey) should be(false)
            ps.hasProperty("aba", superPalindromeKey) should be(false)

            ps.scheduleEagerComputationForEntity("a") { e ⇒
                ps.isSuspended = () ⇒ true
                val dependee = EPK("d", Palindromes.PalindromeKey)
                ps[String, Palindromes.PalindromeProperty](dependee) // we use a fake dependency...
                IntermediateResult(
                    "a",
                    NoPalindrome, Palindrome,
                    Seq(dependee),
                    (eps) ⇒ { Result("a", Palindrome) }
                )
            }
            ps.waitOnPhaseCompletion()
            if (ps.hasProperty("d", palindromeKey)) {
                fail(s"unexpected property: "+ps("d", palindromeKey))
            }

            if (!ps.hasProperty("a", palindromeKey)) {
                ps.isSuspended = () ⇒ false
                ps.waitOnPhaseCompletion()
                ps.hasProperty("a", palindromeKey) should be(true)
            }
        }

        // test SET

        it("set should set an entity's property \"immediately\"") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            val pk = Palindromes.PalindromeKey
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindrome.key), Set.empty)

            ps.set("aba", Palindrome)
            ps.waitOnPhaseCompletion()
            ps("aba", pk) should be(FinalEP("aba", Palindrome))

            ps.set("abca", NoPalindrome)
            ps.waitOnPhaseCompletion()
            ps("abca", pk) should be(FinalEP("abca", NoPalindrome))
        }

        it("should allow setting different properties w.r.t. one entity") {
            import Palindromes.Palindrome
            import Palindromes.SuperPalindrome
            import Palindromes.NoPalindrome
            import Palindromes.NoSuperPalindrome
            val ppk = Palindromes.PalindromeKey
            val sppk = Palindromes.SuperPalindromeKey
            val ps = createPropertyStore()
            ps.setupPhase(Set(Palindrome.key), Set.empty)

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)
            ps.waitOnPhaseCompletion()
            ps("aba", ppk) should be(FinalEP("aba", Palindrome))
            ps("aba", sppk) should be(FinalEP("aba", SuperPalindrome))

            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)
            ps.waitOnPhaseCompletion()
            ps("abca", ppk) should be(FinalEP("abca", NoPalindrome))
            ps("abca", sppk) should be(FinalEP("abca", NoSuperPalindrome))
        }

        it("should be able to enumerate all explicitly set properties of an entity") {
            import Palindromes.Palindrome
            import Palindromes.SuperPalindrome
            import Palindromes.NoPalindrome
            import Palindromes.NoSuperPalindrome
            val ppk = Palindromes.PalindromeKey
            val sppk = Palindromes.SuperPalindromeKey
            val ps = createPropertyStore()
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
        }

        it("should not set an entity's property if it already has a property") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            val ps = createPropertyStore()

            ps.set("aba", Palindrome)

            try {
                ps.set("aba", NoPalindrome)
                ps.waitOnPhaseCompletion()
            } catch {
                case AbortedDueToException(_: IllegalStateException) ⇒ // eager exception : OK
                case _: IllegalStateException                        ⇒ // eager exception : OK
            }
        }

        it("should contain the results (at least) for all entities for which we scheduled a computation") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome

            val ps = createPropertyStore()
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
        }

        it("should trigger a lazy property computation only lazily") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome

            val pk = Palindromes.PalindromeKey
            val ps = createPropertyStore()
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
        }

        it("should not trigger a lazy property computation multiple times") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome

            val pk = Palindromes.PalindromeKey
            val ps = createPropertyStore()
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

            invocationCount.get should be(1)
        }

        it("should complete the computation of dependent lazy computations before the phase ends") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            import Palindromes.NoSuperPalindrome
            import Palindromes.SuperPalindrome
            val ppk = Palindromes.PalindromeKey
            val sppk = Palindromes.SuperPalindromeKey

            val ps = createPropertyStore()
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

                    IntermediateResult(
                        e, NoSuperPalindrome, SuperPalindrome,
                        Seq(initialsExpectedEP),
                        (eps) ⇒ {
                            if (eps.lb == Palindrome /*&& ...*/ )
                                Result(e, SuperPalindrome)
                            else
                                Result(e, NoSuperPalindrome)
                        }
                    )
                }
            )
            ps.scheduleEagerComputationForEntity("e") { e: String ⇒
                val initiallyExpectedEP = EPK("e", sppk)
                ps("e", sppk) should be(initiallyExpectedEP)
                IntermediateResult(
                    "e", Marker.NotMarked, Marker.IsMarked,
                    Seq(initiallyExpectedEP),
                    (eps) ⇒ {
                        // Depending the scheduling, we can have a final result here as well.
                        if (eps.isFinal) {
                            if (eps.lb == SuperPalindrome)
                                Result(e, Marker.IsMarked)
                            else
                                Result(e, Marker.NotMarked)
                        } else
                            IntermediateResult(
                                "e", Marker.NotMarked, Marker.IsMarked,
                                Seq(eps),
                                (eps) ⇒ {
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

            ps("e", ppk) should be(FinalEP("e", Palindrome))
            ps("e", sppk) should be(FinalEP("e", SuperPalindrome))
            ps("e", Marker.MarkerKey) should be(FinalEP("e", Marker.IsMarked))
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
                        (_: PropertyStore, e: Node) ⇒ AllNodes,
                        (_: PropertyStore, eps: EPS[Node, ReachableNodes]) ⇒ eps.ub,
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
            def reachableNodesAnalysis(ps: PropertyStore)(n: Node): PropertyComputationResult = {
                val nTargets = n.targets
                if (nTargets.isEmpty)
                    return Result(n, NoReachableNodes);

                var allDependees: Set[Node] = nTargets.toSet // may include self-dependency
                var dependeePs: Set[EOptionP[Entity, _ <: ReachableNodes]] =
                    ps(allDependees - n /* ignore self-dependencies */ , ReachableNodes.Key).toSet

                // incremental computation
                def c(dependee: SomeEPS): PropertyComputationResult = {
                    // Get the set of currently reachable nodes:
                    val eps @ EPS(_, _ /*lb*/ , ReachableNodes(depeendeeReachableNodes)) = dependee

                    // Compute the new set of reachable nodes:
                    allDependees ++= depeendeeReachableNodes
                    val newUB = ReachableNodes(allDependees)

                    // Adapt the set of dependeePs to ensure termination
                    dependeePs = dependeePs.filter { _.e ne dependee.e }
                    if (!eps.isFinal) {
                        dependeePs ++=
                            Traversable(dependee.asInstanceOf[EOptionP[Entity, _ <: ReachableNodes]])
                    }
                    val r = {
                        if (dependeePs.nonEmpty)
                            IntermediateResult(n, AllNodes, newUB, dependeePs, c)
                        else
                            Result(n, newUB)
                    }
                    r
                }

                // initial computation
                dependeePs foreach { dependee ⇒
                    if (dependee.hasProperty) {
                        if (dependee.isFinal) { dependeePs -= dependee }
                        allDependees ++= dependee.ub.nodes
                    }
                }
                val currentReachableNodes = ReachableNodes(allDependees)
                if (dependeePs.isEmpty)
                    Result(n, currentReachableNodes)
                else
                    IntermediateResult(n, AllNodes, currentReachableNodes, dependeePs, c)
            }

            def reachableNodesCountAnalysis(ps: PropertyStore)(
                n: Node
            ): PropertyComputationResult = {
                var dependees: List[SomeEOptionP] = Nil
                var ub: Int = n.targets.size

                def c(eps: SomeEOptionP): PropertyComputationResult = {
                    eps match {

                        case IntermediateEP(_, _, ReachableNodesCount(otherUB)) ⇒
                            if (ub + otherUB > 4)
                                Result(n, TooManyNodesReachable)
                            else {
                                dependees = eps :: dependees.filter(_.e ne eps.e)
                                IntermediateResult(
                                    n, TooManyNodesReachable, ReachableNodesCount(ub),
                                    dependees,
                                    c
                                )
                            }

                        case FinalEP(_, reachableNodesCount: ReachableNodesCount) ⇒
                            ub += reachableNodesCount.value
                            if (ub > 4) {
                                Result(n, TooManyNodesReachable)
                            } else if (dependees.tail.isEmpty) {
                                Result(n, ReachableNodesCount(ub))
                            } else {
                                dependees = dependees.filter(_.e ne eps.e)
                                IntermediateResult(
                                    n, TooManyNodesReachable, ReachableNodesCount(ub),
                                    dependees,
                                    c
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
                            case iep @ IntermediateEP(_, _, ReachableNodesCount(otherUB)) ⇒
                                if (ub + otherUB > 4) {
                                    ub = TooManyNodesReachable.value
                                    false
                                } else {
                                    // we have to wait for the final value before we can add the count
                                    dependees ::= iep
                                    true
                                }
                            case FinalEP(_, reachableNodesCount) ⇒
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
                    IntermediateResult(
                        n, TooManyNodesReachable, ReachableNodesCount(ub),
                        dependees,
                        c
                    )
            }

            def reachableNodesCountViaReachableNodesAnalysis(ps: PropertyStore)(
                n: Node
            ): PropertyComputationResult = {

                def c(eps: SomeEOptionP): PropertyComputationResult = {
                    eps match {
                        case eps @ IntermediateEP(_, _, ReachableNodes(reachableNodes)) ⇒
                            IntermediateResult(
                                n, TooManyNodesReachable, ReachableNodesCount(reachableNodes.size),
                                List(eps),
                                c
                            )

                        case FinalEP(_, ReachableNodes(reachableNodes)) ⇒
                            Result(n, ReachableNodesCount(reachableNodes.size))
                    }
                }

                ps(n, ReachableNodes.Key) match {
                    case epk: EPK[_, _] ⇒
                        IntermediateResult(
                            n, TooManyNodesReachable, ReachableNodesCount(0),
                            List(epk),
                            c
                        )
                    case eps: SomeEOptionP ⇒ c(eps)

                }
            }
            // the graph:
            // a -> f -> h
            // a -> f -> j
            // a -> f -> i
            //           h <-> j <-> i // this cycle is resolved in multiple steps...
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
                for (nodeEntitiesPermutation ← nodeEntities.permutations.drop(dropCount).take(10)) {

                    val ps = createPropertyStore()
                    ps.setupPhase(Set(ReachableNodesCount.Key, ReachableNodes.Key), Set.empty)
                    ps.registerLazyPropertyComputation(
                        ReachableNodesCount.Key, reachableNodesCountAnalysis(ps)
                    )
                    ps.scheduleEagerComputationsForEntities(nodeEntitiesPermutation)(reachableNodesAnalysis(ps))
                    ps(nodeA, ReachableNodesCount.Key) // forces the evaluation for all nodes...

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

                    info(
                        s"(id of first permutation = ${dropCount + 1}) number of executed tasks:"+ps.scheduledTasksCount+
                            "; number of scheduled onUpdateContinuations:"+ps.scheduledOnUpdateComputationsCount+
                            "; number of eager onUpdateContinuations:"+ps.eagerOnUpdateComputationsCount
                    )
                }
            }

            it("should be possible using lazy scheduled computations") {

                val ps = createPropertyStore()
                ps.setupPhase(Set(ReachableNodesCount.Key, ReachableNodes.Key), Set.empty)
                ps.registerLazyPropertyComputation(
                    ReachableNodes.Key, reachableNodesAnalysis(ps)
                )
                ps(nodeA, ReachableNodes.Key) // forces the evaluation for all nodes...
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
            }

            it("should be possible using lazy scheduled mutually dependent computations") {

                val ps = createPropertyStore()
                ps.setupPhase(Set(ReachableNodes.Key, ReachableNodesCount.Key), Set.empty)
                ps.registerLazyPropertyComputation(
                    ReachableNodes.Key, reachableNodesAnalysis(ps)
                )
                ps.registerLazyPropertyComputation(
                    ReachableNodesCount.Key, reachableNodesCountViaReachableNodesAnalysis(ps)
                )
                nodeEntities.foreach { node ⇒ ps(node, ReachableNodesCount.Key) }
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
                // now let's check if we have the correct notification of the
                // of the lazily dependent computations
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
            }

            it("should be possible when a lazy computation depends on properties for which no analysis is scheduled") {

                val ps = createPropertyStore()
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
            }

            it("should be possible when a lazy computation depends on properties for which analysis seems to be scheduled, but no analysis actually produces results") {

                val ps = createPropertyStore()
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
            object Node { def apply(name: String) = new Node(name) }

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
                    (ps: PropertyStore, e: Entity) ⇒ ???,
                    (ps: PropertyStore, eps: SomeEPS) ⇒ ???,
                    (ps: PropertyStore, e: Entity) ⇒ None
                )
            }
            case class TreeLevel(length: Int) extends Property {
                final type Self = TreeLevel
                final def key = TreeLevelKey
            }

            val ps = createPropertyStore()
            ps.setupPhase(Set(TreeLevelKey), Set.empty)

            /* The following analysis only uses the new information given to it and updates
                 * the set of observed dependees.
                 */
            def analysis(level: Int)(n: Node): PropertyComputationResult = {
                val nextPCs: Traversable[(PropertyComputation[Node], Node)] =
                    n.targets.map(t ⇒ (analysis(level + 1) _, t))
                IncrementalResult(Result(n, TreeLevel(level)), nextPCs)
            }

            ps.scheduleEagerComputationForEntity(nodeRoot)(analysis(0))
            ps.waitOnPhaseCompletion

            ps(nodeRoot, TreeLevelKey) should be(FinalEP(nodeRoot, TreeLevel(0)))
            ps(nodeRRoot, TreeLevelKey) should be(FinalEP(nodeRRoot, TreeLevel(1)))
            ps(nodeRRRoot, TreeLevelKey) should be(FinalEP(nodeRRRoot, TreeLevel(2)))
            ps(nodeLRRoot, TreeLevelKey) should be(FinalEP(nodeLRRoot, TreeLevel(2)))
            ps(nodeLRoot, TreeLevelKey) should be(FinalEP(nodeLRoot, TreeLevel(1)))
            ps(nodeLLRoot, TreeLevelKey) should be(FinalEP(nodeLLRoot, TreeLevel(2)))
        }

        it("should never pass a `PropertyIsLazilyComputed` to clients") {
            val ppk = Palindromes.PalindromeKey

            val ps = createPropertyStore()
            ps.setupPhase(Set(ppk), Set.empty)

            ps.registerLazyPropertyComputation(
                ppk,
                (e: Entity) ⇒ {
                    val p = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                    Result(e, p)
                }
            )
            ps.scheduleEagerComputationForEntity(
                "aaa"
            ) { s: String ⇒
                    ps("a", ppk) match {
                        case epk: EPK[_, _] ⇒
                            IntermediateResult(
                                s, NoPalindrome, Palindrome,
                                List(epk),
                                (eps: SomeEPS) ⇒ {
                                    if (eps.lb == PropertyIsLazilyComputed ||
                                        eps.ub == PropertyIsLazilyComputed)
                                        fail("clients should never see PropertyIsLazilyComputed")
                                    else
                                        Result(s, Palindrome)
                                }
                            )
                        case _ ⇒ fail("unexpected result")
                    }
                }
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
                ps.setupPhase(Set(Purity.Key), Set.empty)

                def purityAnalysis(node: Node): PropertyComputationResult = {
                    def c(successorNode: SomeEOptionP): PropertyComputationResult = {
                        // HERE - For this test case only, we can simple get to the previous
                        // node from the one that was updated.
                        successorNode match {

                            case epk: EPK[_, _] ⇒
                                IntermediateResult(node, Impure, Pure, Iterable(epk), c)

                            case eps @ IntermediateEP(_, lb, ub) ⇒
                                IntermediateResult(node, lb, ub, Iterable(eps), c)

                            // required when we resolve the cycle
                            case FinalEP(_, Pure)             ⇒ Result(node, Pure)

                            // the following cases should never happen...
                            case IntermediateEP(_, Impure, _) ⇒ ???
                            case FinalEP(_, Impure)           ⇒ ???
                        }
                    }: PropertyComputationResult

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

                info(s"test succeeded with $testSize node(s) in a circle")
                info(
                    s"number of executed tasks:"+ps.scheduledTasksCount+
                        "; number of scheduled onUpdateContinuations:"+ps.scheduledOnUpdateComputationsCount+
                        "; number of eager onUpdateContinuations:"+ps.eagerOnUpdateComputationsCount
                )
            }
        }
    }
}

abstract class PropertyStoreTestWithDebugging extends PropertyStoreTest {

    private[this] var oldPropertyStoreUpdateSetting = PropertyStore.Debug
    override def beforeAll(): Unit = PropertyStore.updateDebug(true)
    override def afterAll(): Unit = PropertyStore.updateDebug(oldPropertyStoreUpdateSetting)

    describe("the property store with turned-on debugging support") {

        it("should catch IntermediateResults with inverted property bounds") {
            assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

            val nodeA = Node("a")
            val nodeB = Node("b")

            val ps = createPropertyStore()
            ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)
            def aContinuation(bStringEOptionP: SomeEOptionP): PropertyComputationResult = ???
            def aAnalysis(ignored: Node): PropertyComputationResult = {
                val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                new IntermediateResult(
                    nodeA, ReachableNodesCount(10), ReachableNodesCount(20), List(bEOptionP),
                    aContinuation
                )
            }
            assertThrows[IllegalArgumentException] {
                ps.scheduleEagerComputationForEntity(Node("a"))(aAnalysis)
                ps.waitOnPhaseCompletion()
            }
        }

        it("should catch non-monotonic updates related to the lower bound") {
            assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

            var count = 0
            val nodeA = Node("a")
            val nodeB = Node("b")

            val ps = createPropertyStore()
            ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

            def lazyAnalysis(n: Node): PropertyComputationResult = {
                val aEOptionP = ps(nodeA, ReachableNodesCount.Key)
                count += 1
                new IntermediateResult(
                    n, ReachableNodesCount(100 - count), ReachableNodesCount(count), List(aEOptionP),
                    aContinuation
                )
            }
            def aContinuation(bStringEOptionP: SomeEOptionP): PropertyComputationResult = {
                new IntermediateResult(
                    nodeA,
                    ReachableNodesCount(8), // <= invalid refinement of lower bound!
                    ReachableNodesCount(1),
                    List(bStringEOptionP),
                    aContinuation
                )
            }
            def aAnalysis(string: Node): PropertyComputationResult = {
                val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                new IntermediateResult(
                    nodeA, ReachableNodesCount(20), ReachableNodesCount(0), List(bEOptionP),
                    aContinuation
                )
            }
            ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
            try {
                ps.scheduleEagerComputationForEntity(nodeA)(aAnalysis)
                ps.waitOnPhaseCompletion()
            } catch {
                case _: IllegalArgumentException ⇒ // OK - EXPECTED
                case e: Throwable ⇒
                    e.printStackTrace()
                    fail(s"unexpected exception: ${e.getMessage}")
            }
        }

        it("should catch non-monotonic updates related to the upper bound") {
            assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

            var count = 0
            val nodeA = Node("a")
            val nodeB = Node("b")

            val ps = createPropertyStore()
            ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

            def lazyAnalysis(n: Node): PropertyComputationResult = {
                val aEOptionP = ps(Node("a"), ReachableNodesCount.Key)
                count += 1
                new IntermediateResult(
                    n, ReachableNodesCount(100 - count), ReachableNodesCount(count), List(aEOptionP),
                    aContinuation
                )
            }
            def aContinuation(bStringEOptionP: SomeEOptionP): PropertyComputationResult = {
                new IntermediateResult(
                    nodeA,
                    ReachableNodesCount(21),
                    ReachableNodesCount(0), // <= invalid refinement of upper bound!
                    List(bStringEOptionP),
                    aContinuation
                )
            }
            def aAnalysis(ignored: Node): PropertyComputationResult = {
                val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                new IntermediateResult(
                    nodeA, ReachableNodesCount(20), ReachableNodesCount(1), List(bEOptionP),
                    aContinuation
                )
            }
            ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
            assertThrows[IllegalArgumentException] {
                ps.scheduleEagerComputationForEntity(nodeA)(aAnalysis)
                ps.waitOnPhaseCompletion()
            }
        }

        it("should catch updates when the upper bound is lower than the lower bound") {
            assert(PropertyStore.Debug, "debugging is turned off") // test the pre-state

            var count = 0
            val nodeA = Node("a")
            val nodeB = Node("b")

            val ps = createPropertyStore()
            ps.setupPhase(Set(ReachableNodesCount.Key), Set.empty)

            def lazyAnalysis(n: Node): PropertyComputationResult = {
                val aEOptionP = ps(nodeA, ReachableNodesCount.Key)
                count += 1
                new IntermediateResult(
                    n, ReachableNodesCount(100 - count), ReachableNodesCount(count), List(aEOptionP),
                    aContinuation
                )
            }
            def aContinuation(bStringEOptionP: SomeEOptionP): PropertyComputationResult = {
                new IntermediateResult(
                    nodeA,
                    // bounds have surpassed themselve
                    ReachableNodesCount(5),
                    ReachableNodesCount(15),
                    List(bStringEOptionP),
                    aContinuation
                )
            }
            def aAnalysis(string: Node): PropertyComputationResult = {
                val bEOptionP = ps(nodeB, ReachableNodesCount.Key)
                new IntermediateResult(
                    nodeA, ReachableNodesCount(20), ReachableNodesCount(1), List(bEOptionP),
                    aContinuation
                )
            }
            ps.registerLazyPropertyComputation(ReachableNodesCount.Key, lazyAnalysis)
            assertThrows[IllegalArgumentException] {
                ps.scheduleEagerComputationForEntity(nodeA)(aAnalysis)
                ps.waitOnPhaseCompletion()
            }
        }
    }
}

abstract class PropertyStoreTestWithoutDebugging extends PropertyStoreTest {
    private[this] var oldPropertyStoreUpdateSetting = PropertyStore.Debug
    override def beforeAll(): Unit = PropertyStore.updateDebug(false)
    override def afterAll(): Unit = PropertyStore.updateDebug(oldPropertyStoreUpdateSetting)
}

// Test fixture related to a simple marker property
object Marker {
    final val MarkerKey = {
        PropertyKey.create[Entity, MarkerProperty](
            "Marker",
            (ps: PropertyStore, e: Entity) ⇒ NotMarked,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???,
            (ps: PropertyStore, e: Entity) ⇒ None
        )
    }

    sealed trait MarkerProperty extends Property {
        type Self = MarkerProperty
        def key = MarkerKey
    }
    case object IsMarked extends MarkerProperty
    case object NotMarked extends MarkerProperty
}

// Test fixture related to Palindromes.
object Palindromes {

    final val PalindromeKey = {
        PropertyKey.create[Entity, PalindromeProperty](
            "Palindrome",
            (ps: PropertyStore, e: Entity) ⇒ NoPalindrome,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???,
            (ps: PropertyStore, e: Entity) ⇒ None
        )
    }

    sealed trait PalindromeProperty extends Property {
        type Self = PalindromeProperty
        def key = PalindromeKey
    }
    case object Palindrome extends PalindromeProperty
    case object NoPalindrome extends PalindromeProperty

    // We consider a Palindrome a SuperPalindrome if also the first half
    // is a Palindrome. If the entities' size is odd, the middle element
    // is ignored.
    final val SuperPalindromeKey = {
        PropertyKey.create[Entity, SuperPalindromeProperty](
            "SuperPalindrome",
            (ps: PropertyStore, e: Entity) ⇒ NoSuperPalindrome,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???,
            (ps: PropertyStore, e: Entity) ⇒ None
        )
    }

    sealed trait SuperPalindromeProperty extends Property {
        type Self = SuperPalindromeProperty
        def key = SuperPalindromeKey
    }
    case object SuperPalindrome extends SuperPalindromeProperty
    case object NoSuperPalindrome extends SuperPalindromeProperty
}

sealed trait Purity extends OrderedProperty {
    final type Self = Purity
    final def key = Purity.Key
}
object Purity {
    final val Key = PropertyKey.create[Entity, Purity]("Purity", Impure)
}
case object Pure extends Purity {
    def checkIsEqualOrBetterThan(e: Entity, other: Purity): Unit = { /* always true */ }
}
case object Impure extends Purity {
    def checkIsEqualOrBetterThan(e: Entity, other: Purity): Unit = {
        if (other != Impure) {
            throw new IllegalArgumentException(s"$e: $this is not equal or better than $other")
        }
    }
}

class Node(
        val name:    String,
        val targets: scala.collection.mutable.Set[Node] = scala.collection.mutable.Set.empty
) {

    override def hashCode: Int = name.hashCode()
    override def equals(other: Any): Boolean = other match {
        case that: Node ⇒ this.name equals that.name
        case _          ⇒ false
    }

    override def toString: String = name // RECALL: Nodes are potentially used in cycles.
}
object Node { def apply(name: String) = new Node(name) }

object ReachableNodesCount {
    val Key: PropertyKey[ReachableNodesCount] =

        PropertyKey.create[Node, ReachableNodesCount](
            s"ReachableNodesCount",
            (_: PropertyStore, e: Node) ⇒ TooManyNodesReachable,
            (_: PropertyStore, eps: EPS[Node, ReachableNodesCount]) ⇒ TooManyNodesReachable,
            (ps: PropertyStore, e: Entity) ⇒ None
        )
}
case class ReachableNodesCount(value: Int) extends OrderedProperty {
    type Self = ReachableNodesCount
    def key = ReachableNodesCount.Key

    def checkIsEqualOrBetterThan(e: Entity, other: ReachableNodesCount): Unit = {
        if (this.value > other.value) {
            throw new IllegalArgumentException(s"$e: $this is not equal or better than $other")
        }
    }

}

object NoNodesReachable extends ReachableNodesCount(0) {
    override def toString: String = "NoNodesReachable"
}
object TooManyNodesReachable extends ReachableNodesCount(64) {
    override def toString: String = "TooManyNodesReachable"
}
