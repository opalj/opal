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

import java.util.concurrent.atomic.AtomicInteger

import org.junit.runner.RunWith
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

    import Palindromes.NoPalindrome
    import Palindromes.Palindrome

    implicit val logContext = GlobalLogContext

    def createPropertyStore(): PropertyStore

    describe("the property store") {

        it("directly after creation it should be empty (entities(...) and properties(...))") {
            val ps = createPropertyStore()
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

            ps.scheduleForEntity("a") { e ⇒
                ps.isInterrupted = () ⇒ true
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
            ps.scheduleForEntity("d")(e ⇒ Result("d", Palindrome))

            ps("a", Palindromes.PalindromeKey) should be(IntermediateEP("a", NoPalindrome, Palindrome))
            ps("d", Palindromes.PalindromeKey) should be(EPK("d", Palindromes.PalindromeKey))

            // let's test that – if we resume the computation – the results are as expected!
            ps.isInterrupted = () ⇒ false
            ps.waitOnPhaseCompletion()
            ps("a", Palindromes.PalindromeKey) should be(FinalEP("a", Palindrome))
            ps("d", Palindromes.PalindromeKey) should be(FinalEP("d", Palindrome))
        }

        it("should be able to perform queries w.r.t. unknown entities") {
            val ps = createPropertyStore()
            val pk = Palindromes.PalindromeKey

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
            ps.hasProperty("aba", palindromeKey) should be(true)
            ps.hasProperty("cbc", palindromeKey) should be(false)
            ps.hasProperty("aba", superPalindromeKey) should be(false)

            ps.scheduleForEntity("a") { e ⇒
                ps.isInterrupted = () ⇒ true
                val dependee = EPK("d", Palindromes.PalindromeKey)
                ps(dependee) // we use a fake dependency...
                IntermediateResult(
                    "a",
                    NoPalindrome, Palindrome,
                    Seq(dependee),
                    (eps) ⇒ { Result("a", Palindrome) }
                )
            }
            ps.waitOnPhaseCompletion()
            ps.hasProperty("a", palindromeKey) should be(true)
            ps.hasProperty("d", palindromeKey) should be(false)
        }

        // test SET

        it("set should set an entity's property immediately") {
            import Palindromes.Palindrome
            import Palindromes.NoPalindrome
            val pk = Palindromes.PalindromeKey
            val ps = createPropertyStore()

            ps.set("aba", Palindrome)
            ps("aba", pk) should be(FinalEP("aba", Palindrome))

            ps.set("abca", NoPalindrome)
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

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)

            ps("aba", ppk) should be(FinalEP("aba", Palindrome))
            ps("aba", sppk) should be(FinalEP("aba", SuperPalindrome))

            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)
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

            ps.set("aba", Palindrome)
            ps.set("aba", SuperPalindrome)
            ps.set("abca", NoPalindrome)
            ps.set("abca", NoSuperPalindrome)

            ps.entities(ppk).map(_.e).toSet should be(Set("aba", "abca"))
            ps.entities(sppk).map(_.e).toSet should be(Set("aba", "abca"))

            ps.properties("aba").toSet should be(Set(FinalEP("aba", Palindrome), FinalEP("aba", SuperPalindrome)))
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
            ps.setupPhase(Set(Palindromes.PalindromeKey), Set.empty)

            val pk = Palindromes.PalindromeKey
            val es = Set("aba", "cc", "d", "fd", "zu", "aaabbbaaa")
            ps.scheduleForEntities(es) { e ⇒
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
            ps.scheduleForEntity("e") { e: String ⇒
                val initiallyExpectedEP = EPK("e", sppk)
                ps("e", sppk) should be(initiallyExpectedEP)
                IntermediateResult(
                    "e", Marker.NotMarked, Marker.IsMarked,
                    Seq(initiallyExpectedEP),
                    (eps) ⇒ {
                        if (eps.isFinal)
                            fail("premature final value")

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

        it("should be possible to have computations with multiple updates") {

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

            // DESCRIPTION OF A GRAPH (WITH CYCLES)
            val nodeA = Node("a")
            val nodeB = Node("b")
            val nodeC = Node("c")
            val nodeD = Node("d")
            val nodeE = Node("e")
            val nodeF = Node("f")
            val nodeG = Node("g")
            val nodeH = Node("h")
            val nodeR = Node("r")
            nodeA.targets += nodeB // the graph:
            nodeA.targets += nodeF // a -> f
            nodeF.targets += nodeH //      f -> h
            nodeA.targets += nodeG // a -> g
            nodeG.targets += nodeH //      g -> h
            nodeA.targets += nodeH // a -> h
            nodeB.targets += nodeC // a -> b -> c
            nodeB.targets += nodeD //        ↘︎  d
            nodeD.targets += nodeD //           d ⟲
            nodeD.targets += nodeE //           d -> e
            nodeE.targets += nodeR //                e -> r
            nodeR.targets += nodeB //       ↖︎------------↵︎
            val nodeEntities = List[Node](
                nodeA, nodeB, nodeC, nodeD, nodeE, nodeF, nodeG, nodeH, nodeR
            )

            object ReachableNodes {
                val Key: PropertyKey[ReachableNodes] =

                    PropertyKey.create[Node, ReachableNodes](
                        "ReachableNodes",
                        (_: PropertyStore, e: Node) ⇒ AllNodes,
                        (_: PropertyStore, eps: EPS[Node, ReachableNodes]) ⇒ eps.toUBEP
                    )
            }
            case class ReachableNodes(nodes: scala.collection.Set[Node]) extends Property {
                type Self = ReachableNodes
                def key = ReachableNodes.Key
            }
            object NoReachableNodes extends ReachableNodes(Set.empty) {
                override def toString: String = "NoReachableNodes"
            }
            object AllNodes extends ReachableNodes(nodeEntities.toSet) {
                override def toString: String = "AllNodes"
            }

            val ps = createPropertyStore()

            /* The following analysis only uses the new information given to it and updates
                                 * the set of observed dependees.
                                 */
            def analysis(n: Node): PropertyComputationResult = {
                val nTargets = n.targets
                if (nTargets.isEmpty)
                    return Result(n, NoReachableNodes);

                var allDependees: mutable.Set[Node] =
                    nTargets.clone - n // self-dependencies are ignored!
                var dependeePs: Set[EOptionP[Entity, _ <: ReachableNodes]] =
                    ps(allDependees, ReachableNodes.Key).toSet

                // incremental computation
                def c(dependee: SomeEPS): PropertyComputationResult = {
                    // Get the set of currently reachable nodes:
                    val eps @ EPS(_, _ /*lb*/ , ReachableNodes(depeendeeReachableNodes)) = dependee

                    // Compute the new set of reachable nodes:
                    allDependees = allDependees ++ depeendeeReachableNodes
                    val newUB = ReachableNodes(allDependees)

                    // Adapt the set of dependeePs to ensure termination
                    dependeePs = dependeePs.filter { _.e ne dependee.e }
                    if (!eps.isFinal) {
                        dependeePs ++= Traversable(dependee.asInstanceOf[EOptionP[Entity, _ <: ReachableNodes]])
                    }
                    if (dependeePs.nonEmpty)
                        IntermediateResult(n, AllNodes, newUB, dependeePs, c)
                    else
                        Result(n, newUB)
                }

                // initial computation
                val reachableNodes =
                    dependeePs.foldLeft(allDependees.clone) { (reachableNodes, dependee) ⇒
                        if (dependee.hasProperty) {
                            if (dependee.isFinal) { dependeePs -= dependee }
                            reachableNodes ++ dependee.ub.nodes
                        } else {
                            reachableNodes
                        }
                    }
                val currentReachableNodes = ReachableNodes(
                    if (n.targets contains n)
                        reachableNodes + n
                    else
                        reachableNodes
                )
                if (dependeePs.isEmpty)
                    Result(n, currentReachableNodes)
                else
                    IntermediateResult(n, AllNodes, currentReachableNodes, dependeePs, c)
            }

            ps.scheduleForEntities(nodeEntities)(analysis)
            ps.waitOnPhaseCompletion()

            // the graph:
            // a -> f -> h
            // a -> g -> h
            // a -> h
            // a -> b -> c
            //      b -> d
            //           d ⟲
            //           d -> e
            //                e -> r
            //       ↖︎----------< r
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
                    "TreeLevel",
                    (ps: PropertyStore, e: Entity) ⇒ ???,
                    (ps: PropertyStore, eps: SomeEPS) ⇒ ???
                )
            }
            case class TreeLevel(length: Int) extends Property {
                final type Self = TreeLevel
                final def key = TreeLevelKey
                final def isRefineable = false
            }

            val ps = createPropertyStore()

            /* The following analysis only uses the new information given to it and updates
                 * the set of observed dependees.
                 */
            def analysis(level: Int)(n: Node): PropertyComputationResult = {
                val nextPCs: Traversable[(PropertyComputation[Node], Node)] =
                    n.targets.map(t ⇒ (analysis(level + 1) _, t))
                IncrementalResult(Result(n, TreeLevel(level)), nextPCs)
            }

            // the dot in ".<||<" is necessary to shut-up scalariform...
            ps.scheduleForEntity(nodeRoot)(analysis(0))
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
            ps.scheduleForEntity(
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
                val store = createPropertyStore()
                store.setupPhase(Set(Purity.Key), Set.empty)

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

                            // the following three cases should never happen...
                            case IntermediateEP(_, Impure, _) ⇒ ???
                            case FinalEP(_, Impure)           ⇒ ???
                        }
                    }: PropertyComputationResult

                    val nextNode = node.targets.head // HERE: we always have only one successor
                    c(store(nextNode, Purity.Key))
                }
                // 4. execute analysis
                store.scheduleForEntities(allNodes)(purityAnalysis)
                store.waitOnPhaseCompletion()

                // 5. let's evaluate the result
                store.entities(Purity.Key) foreach { eps ⇒
                    if (eps.lb != Pure) {
                        info(store.toString(true))
                        fail(s"Node(${eps.e}) is not Pure (${eps.lb})")
                    }
                }

                info(s"test succeeded with $testSize node(s) in a circle")
            }
        }
    }
}

// Test fixture related to a simple marker property
object Marker {
    final val MarkerKey = {
        PropertyKey.create[Entity, MarkerProperty](
            "Marker",
            (ps: PropertyStore, e: Entity) ⇒ NotMarked,
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???
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
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???
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
            (ps: PropertyStore, eOptionP: SomeEOptionP) ⇒ ???
        )
    }

    sealed trait SuperPalindromeProperty extends Property {
        type Self = SuperPalindromeProperty
        def key = SuperPalindromeKey
    }
    case object SuperPalindrome extends SuperPalindromeProperty
    case object NoSuperPalindrome extends SuperPalindromeProperty
}

sealed trait Purity extends Property {
    final type Self = Purity
    final def key = Purity.Key
}
object Purity {
    final val Key = PropertyKey.create[Entity, Purity]("Purity", Impure)
}
case object Pure extends Purity
case object Impure extends Purity

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
