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
package fpcf

import scala.collection.mutable
import scala.collection.{Set ⇒ SomeSet}

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
class LockBasedPropertyStoreTest extends FunSpec with Matchers with BeforeAndAfterEach {

    // The following import is REQUIRED to override the type alias "type (scala.)String = java.lang.String"!
    // otherwise the type derived by the typeTag used as the context object is not
    import java.lang.String

    final val TestDuration /*in minutes*/ = 3.0d / 60.0d

    //**********************************************************************************************
    //
    // TEST FIXTURE
    //

    final val stringEntities: List[String] = List(
        "a", "b", "c",
        "aa", "bb", "cc", "ab", "bc", "cd",
        "aaa", "aea",
        "aabbcbbaa",
        "aaaffffffaaa", "aaaffffffffffffffffaaa"
    )
    var psStrings: LockBasedPropertyStore = initPSStrings()
    def initPSStrings(): LockBasedPropertyStore = {
        val contextObject = "StringEntities"
        implicit val logContext = GlobalLogContext
        val ps = LockBasedPropertyStore(stringEntities, () ⇒ false, context = contextObject)
        assert(ps.context[String] === contextObject)
        ps
    }

    // The resulting collection has > 200 000 entities!
    val stringsAndSetsOfStrings = {
        val rawEntities = stringEntities ++ List("d", "e", "f", "g")
        rawEntities ++ rawEntities.toSet.subsets
    }
    var psStringsAndSetsOfStrings: LockBasedPropertyStore = initPSStringsAndSetsOfStrings()
    def initPSStringsAndSetsOfStrings(): LockBasedPropertyStore = {
        val contextObject = "StringAndSetOfStringsEntities"
        implicit val logContext = GlobalLogContext

        val ps = LockBasedPropertyStore(stringsAndSetsOfStrings, () ⇒ false, context = contextObject)
        assert(ps.context[String] === contextObject)
        ps
    }

    final val PalindromeKey = {
        PropertyKey.create[PalindromeProperty](
            "Palindrome",
            (ps: PropertyStore, e: Entity) ⇒ PalindromeIncomputable,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    sealed trait PalindromeProperty extends Property {
        type Self = PalindromeProperty
        def key = PalindromeKey
    }
    // Multiple properties can share the same property instance
    case object Palindrome extends PalindromeProperty {
        def isRefinable: Boolean = false
    }
    case object NoPalindrome extends PalindromeProperty {
        def isRefinable: Boolean = false
    }
    case object MaybePalindrome extends PalindromeProperty {
        def isRefinable: Boolean = true
    }
    case object PalindromeIncomputable extends PalindromeProperty {
        def isRefinable: Boolean = false
    }

    /**
     * The number of bits of something...
     */
    final val BitsKey = {
        PropertyKey.create[BitsProperty](
            "Bits",
            (ps: PropertyStore, e: Entity) ⇒ ???,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    case class BitsProperty(bits: Int) extends Property {
        type Self = BitsProperty
        def key = BitsKey
        def isRefinable: Boolean = false
    }

    // HERE: we consider a palindrome to be a super palindrome if the lead is itself a
    //         a palindrom. E.g. aa => Lead: a => Palindrome => aa => SuperPalindrome
    final val SuperPalindromeKey = {
        PropertyKey.create[SuperPalindromeProperty](
            "SuperPalindrome",
            (ps: PropertyStore, e: Entity) ⇒ ???,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    sealed trait SuperPalindromeProperty extends Property {
        type Self = SuperPalindromeProperty
        def key = SuperPalindromeKey
        def isRefinable: Boolean = false
    }
    // Multiple properties can share the same property instance
    case object SuperPalindrome extends SuperPalindromeProperty
    case object NoSuperPalindrome extends SuperPalindromeProperty

    final val TaintedKey = {
        PropertyKey.create[TaintedProperty](
            "Tainted",
            (ps: PropertyStore, e: Entity) ⇒ Tainted,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    sealed trait TaintedProperty extends Property {
        type Self = TaintedProperty
        def key = TaintedKey
    }
    // Multiple properties can share the same property instance
    case object Tainted extends TaintedProperty {
        def isRefinable: Boolean = false
    }
    case object NotTainted extends TaintedProperty {
        def isRefinable: Boolean = false
    }
    case object MaybeTainted extends TaintedProperty {
        def isRefinable: Boolean = true
    }

    final val StringLengthKey: PropertyKey[StringLength] = {
        PropertyKey.create(
            "StringLength",
            (ps: PropertyStore, e: Entity) ⇒ ???,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    case class StringLength(length: Int) extends Property {
        final type Self = StringLength
        final def key = StringLengthKey
        final def isRefinable: Boolean = false
    }

    final val CountKey: PropertyKey[Count] = {
        PropertyKey.create(
            "Count",
            (ps: PropertyStore, e: Entity) ⇒ ???,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    case class Count(count: Int) extends Property {
        final type Self = Count
        final def key = CountKey
        final def isRefinable: Boolean = true
    }

    final val PurityKey: PropertyKey[Purity] = {
        PropertyKey.create(
            "Purity",
            (ps: PropertyStore, e: Entity) ⇒
                throw new UnknownError(s"no fallback available for $e"),
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒
                Iterable(Result(epks.head.e, Pure))
        )
    }
    sealed trait Purity extends Property {
        final type Self = Purity
        final def key = PurityKey
    }
    case object Pure extends Purity { final override def isRefinable: Boolean = false }
    case object Impure extends Purity { final override def isRefinable: Boolean = false }
    case object ConditionallyPure extends Purity { final override def isRefinable: Boolean = true }

    class Node(val name: String, val targets: mutable.Set[Node] = mutable.Set.empty) {
        override def hashCode: Int = name.hashCode()
        override def equals(other: Any): Boolean = other match {
            case that: Node ⇒ this.name equals that.name
            case _          ⇒ false
        }
        //override def toString: String = s"""$name->{${targets.map(_.name).mkString(",")}}"""
        override def toString: String = name
    }
    object Node { def apply(name: String) = new Node(name) }

    // DESCRIPTION OF A GRAPH (WITH CYCLES)
    val nodeA = Node("a")
    val nodeB = Node("b")
    val nodeC = Node("c")
    val nodeD = Node("d")
    val nodeE = Node("e")
    val nodeR = Node("R")
    nodeA.targets += nodeB // the graph:
    nodeB.targets += nodeC // a -> b -> c
    nodeB.targets += nodeD //        ↘︎ d
    nodeD.targets += nodeD //           d ⟲
    nodeD.targets += nodeE //           d -> e
    nodeE.targets += nodeR //                e -> r
    nodeR.targets += nodeB //       ↖︎-----------↵︎
    val nodeEntities = List[Node](nodeA, nodeB, nodeC, nodeD, nodeE, nodeR)
    var psNodes: LockBasedPropertyStore = initPSNodes
    def initPSNodes(): LockBasedPropertyStore = {
        psNodes = LockBasedPropertyStore(nodeEntities, () ⇒ false)(GlobalLogContext)
        psNodes
    }

    final val ReachableNodesKey: PropertyKey[ReachableNodes] = {
        PropertyKey.create(
            "ReachableNodes",
            (ps: PropertyStore, e: Entity) ⇒ {
                /*IDIOM IF NO FALLBACK IS EXPECTED/SUPPORTED*/
                throw new UnknownError(s"no fallback for ReachableNodes property for $e available")
            },
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ {
                //                val allReachableNodes = epks.foldLeft(Set.empty[Node]) { (c, epk) ⇒
                //                    c ++ ps(epk.e, ReachableNodesKey /* <=> epk.pk */ ).get.nodes
                //                }
                //                val epk = epks.head
                //                Iterable(Result(epk.e, ReachableNodes(allReachableNodes)))
                // Cycles are completely handled by the analysis;
                // i.e., we do not need to lift the results!
                Iterable.empty
            }
        )
    }
    case class ReachableNodes(nodes: scala.collection.Set[Node]) extends Property {
        type Self = ReachableNodes
        def key = ReachableNodesKey
        def isRefinable = true
    }
    object NoReachableNodes extends ReachableNodes(Set.empty) {
        override def toString: String = "NoReachableNodes"
    }

    if (NoReachableNodes != NoReachableNodes) fail("comparison of ReachableNodes properties failed")

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
    val treeEntities = List[Node](nodeRoot, nodeLRoot, nodeLLRoot, nodeRRoot, nodeLRRoot, nodeRRRoot)
    var treeNodes: LockBasedPropertyStore = initTreeNodes
    def initTreeNodes(): LockBasedPropertyStore = {
        treeNodes = LockBasedPropertyStore(treeEntities, () ⇒ false)(GlobalLogContext)
        treeNodes
    }

    final val TreeLevelKey: PropertyKey[TreeLevel] = {
        PropertyKey.create(
            "TreeLevel",
            (ps: PropertyStore, e: Entity) ⇒ ???,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
        )
    }
    case class TreeLevel(length: Int) extends Property {
        final type Self = TreeLevel
        final def key = TreeLevelKey
        final def isRefinable = false
    }

    override def afterEach(): Unit = {
        if (psStrings.isShutdown) {
            info("reinitializing string entities property store")
            initPSStrings()
        } else {
            psStrings.waitOnPropertyComputationCompletion(false)
            psStrings.reset()
        }

        if (psStringsAndSetsOfStrings.isShutdown) {
            info("reinitializing string entities property store")
            initPSStringsAndSetsOfStrings()
        } else {
            psStringsAndSetsOfStrings.waitOnPropertyComputationCompletion(false)
            psStringsAndSetsOfStrings.reset()
        }

        if (psNodes.isShutdown) {
            info("reinitializing graph nodes property store")
            initPSNodes()
        } else {
            psNodes.waitOnPropertyComputationCompletion(false)
            psNodes.reset()
        }

        if (treeNodes.isShutdown) {
            info("reinitializing tree nodes property store")
            initTreeNodes()
        } else {
            treeNodes.waitOnPropertyComputationCompletion(false)
            treeNodes.reset()
        }
    }

    //**********************************************************************************************
    //
    // TESTS

    describe("the property store") {

        it("should be in the initial state after calling reset") {
            val ps = psStrings

            // let's fill the property store with:
            //  - an entity based property and
            //  - an on property derivation function
            ps schedule { e: Entity ⇒
                val property = if (e.toString.reverse == e.toString) Palindrome else NoPalindrome
                Result(e, property)
            }
            ps.waitOnPropertyComputationCompletion(true)

            // let's test the reset method
            ps.entities { x ⇒ true } should not be ('isEmpty)
            ps.reset()
            ps.entities { x ⇒ true } should be('isEmpty)

        }
    }

    it("should be possible to interrupt the computations") {
        val EntitiesCount = 100000
        val entities: List[java.lang.Integer] = (1 to EntitiesCount).map(Integer.valueOf).toList

        val triggeredComputations = new java.util.concurrent.atomic.AtomicInteger(0)
        @volatile var doInterrupt = false
        val ps = LockBasedPropertyStore(entities, () ⇒ doInterrupt)(GlobalLogContext)
        ps schedule { e: Entity ⇒
            triggeredComputations.incrementAndGet()
            Thread.sleep(50)
            Result(e, BitsProperty(Integer.bitCount(e.asInstanceOf[Integer])))
        }
        doInterrupt = true
        ps.waitOnPropertyComputationCompletion(false)

        if (triggeredComputations.get == EntitiesCount) fail("interrupting the computations failed")
    }

    describe("per entity properties") {

        describe("properties") {

            it("every element can have an individual property instance") {
                val ps = psStrings

                ps schedule { e: Entity ⇒ Result(e, StringLength(e.toString.length())) }

                ps.waitOnPropertyComputationCompletion(true)

                stringEntities.foreach { e ⇒ ps(e, StringLengthKey).p.length should be(e.length()) }
            }
        }

        describe("default property values") {

            it("should be possible to execute an analysis that uses properties for which we have no analysis running and, hence, for which the default property value needs to be used") {

                val ps = psStrings

                // Idea... only if data is tainted we check if it is a palindrome...

                ps schedule { e: Entity ⇒
                    IntermediateResult(
                        EP(e, MaybePalindrome),
                        Seq(ps(e, TaintedKey)),
                        (Entity, Property, UpdateType) ⇒ { NoResult }: PropertyComputationResult
                    )
                }

                ps.waitOnPropertyComputationCompletion(true)

                stringEntities.foreach { e ⇒
                    ps(e, TaintedKey).p should be(Tainted)
                }
            }
        }

        describe("computations for groups of entities") {

            it("should be executed for each group in parallel") {
                val ps = psStrings

                ps.execute({ case s: String ⇒ s }, { (s: String) ⇒ s.length }) { (k, es) ⇒
                    es.map(e ⇒ EP(e, StringLength(k)))
                }

                ps.waitOnPropertyComputationCompletion(true)

                stringEntities.foreach { e ⇒
                    ps(e, StringLengthKey).p.length should be(e.length())
                }
            }

        }

        describe("computations depending on a specific property") {

            it("should be triggered for every entity that already has the respective property") {
                import scala.collection.mutable
                val ps = psStrings
                val results = mutable.Map.empty[Property, mutable.Set[String]]

                ps schedule { e: Entity ⇒
                    if (e.toString.reverse == e.toString())
                        Result(e, Palindrome)
                    else
                        Result(e, NoPalindrome)
                }

                ps.onPropertyChange(PalindromeKey)((e, p) ⇒ results.synchronized {
                    results.getOrElseUpdate(p, mutable.Set.empty[String]).add(e.toString())
                })

                ps.waitOnPropertyComputationCompletion(true)

                val expectedResult = Set(
                    "aabbcbbaa", "aa",
                    "c", "aea", "aaa",
                    "aaaffffffaaa",
                    "aaaffffffffffffffffaaa",
                    "cc", "a", "bb", "b"
                )
                results(Palindrome) should be(expectedResult)
            }

            it("should be triggered for every entity that is associated with the respective property after registering the onPropertyChange function") {
                import scala.collection.mutable
                val ps = psStrings
                val results = mutable.Map.empty[Property, mutable.Set[String]]

                ps.onPropertyChange(PalindromeKey)((e, p) ⇒ results.synchronized {
                    results.getOrElseUpdate(p, mutable.Set.empty).add(e.toString())
                })

                ps schedule { e: Entity ⇒
                    Result(
                        e,
                        if (e.toString.reverse == e.toString())
                            Palindrome
                        else
                            NoPalindrome
                    )
                }

                ps.waitOnPropertyComputationCompletion(true)

                val expectedResult = Set(
                    "aabbcbbaa", "aa", "c", "aea",
                    "aaa", "aaaffffffaaa",
                    "aaaffffffffffffffffaaa", "cc", "a", "bb", "b"
                )
                results(Palindrome) should be(expectedResult)
            }
        }

        describe("computations which derive a primary result and also secondary results") {

            it("should be possible to store some \"final\" information about some properties concurrently") {
                val ps = psStringsAndSetsOfStrings
                ps.scheduleForCollected { case s: Set[String @unchecked] ⇒ s } { s: Set[String] ⇒
                    // the following property is derived concurrently and multiple
                    // times
                    s.foreach { e ⇒
                        ps.put(e, if (e.toString.reverse == e.toString) Palindrome else NoPalindrome)
                    }
                    Result(s, BitsProperty(Integer.bitCount(s.size)))
                }

                ps.waitOnPropertyComputationCompletion(true)

                val expectedResult = mutable.SortedSet(
                    "a", "b", "c", "d", "e", "f", "g",
                    "aa", "cc", "bb",
                    "aaa", "aea",
                    "aabbcbbaa",
                    "aaaffffffaaa",
                    "aaaffffffffffffffffaaa"
                )
                mutable.SortedSet.empty[String] ++ ps.entities(Palindrome) should be(expectedResult)
            }

        }

        describe("computations waiting on a specific property") {

            it("should be triggered for externally computed and then added properties") {
                import scala.collection.mutable
                val ps = psStrings
                val results = mutable.Map.empty[Property, mutable.Set[String]]

                ps.onPropertyChange(PalindromeKey)((e, p) ⇒ results.synchronized {
                    results.getOrElseUpdate(p, mutable.Set.empty).add(e.toString())
                })

                val palindromeProperties = stringEntities.map { e ⇒
                    EP(e, if (e == e.reverse) Palindrome else NoPalindrome)
                }
                ps.set(palindromeProperties) // <= externally computed

                ps.waitOnPropertyComputationCompletion(true)

                val expectedPalindromes = Set(
                    "aabbcbbaa", "aa", "c", "aea",
                    "aaa", "aaaffffffaaa",
                    "aaaffffffffffffffffaaa", "cc", "a", "bb", "b"
                )
                results(Palindrome) should be(expectedPalindromes)
                results(NoPalindrome) should be(Set("ab", "bc", "cd"))
            }

            it("should be possible to execute a dependent analysis where the dependee is scheduled later") {
                val ps = psStrings

                // Here, only if data is tainted we check if it is a palindrome...
                ps schedule { e: Entity ⇒
                    def c: OnUpdateContinuation = (e: Entity, p: Property, ut: UpdateType) ⇒ {
                        if (p == Tainted) {
                            if (e.toString.reverse == e.toString())
                                Result(e, Palindrome)
                            else
                                Result(e, NoPalindrome)
                        } else if (p == MaybeTainted) {
                            IntermediateResult(
                                e, MaybePalindrome,
                                Seq(EP(e, p)),
                                c
                            )
                        } else {
                            NoResult // => let's kill the computation
                        }
                    }: PropertyComputationResult

                    IntermediateResult(
                        EP(e, MaybePalindrome), Seq(ps(e, TaintedKey)),
                        c
                    )
                }

                ps schedule { e: Entity ⇒
                    IntermediateResult(
                        EP(e, MaybeTainted), Seq(ps(e, StringLengthKey)),
                        (e: Entity, p: Property, UpdateType) ⇒ {
                            if (p.asInstanceOf[StringLength].length % 2 == 0) {
                                Result(e, Tainted)
                            } else {
                                Result(e, NotTainted)
                            }
                        }: PropertyComputationResult
                    )
                }

                ps schedule { e: Entity ⇒ Result(e, StringLength(e.toString.length())) }

                ps.waitOnPropertyComputationCompletion(true)

                stringEntities.foreach { e ⇒
                    ps(e, TaintedKey).p should be(if (e.length % 2 == 0) Tainted else NotTainted)
                    ps(e, StringLengthKey).p should be(StringLength(e.length))
                    if (ps(e, TaintedKey).p == NotTainted) {
                        ps(e, PalindromeKey) should be(EP(e, MaybePalindrome))
                    }
                }

            }

            it("should be possible to execute an analysis incrementally using the standard scheduling funcation <||<") {
                val ps = treeNodes

                var runs = 0
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < TestDuration * 60 * 1000) {
                    runs += 1
                    /* The following analysis only uses the new information given to it and updates
                     * the set of observed dependees.
                     */
                    def analysis(level: Int)(n: Node): PropertyComputationResult = {
                        val nextPCs: Traversable[(PropertyComputation[Node], Node)] =
                            n.targets.map(t ⇒ (analysis(level + 1) _, t))
                        IncrementalResult(Result(n, TreeLevel(level)), nextPCs)
                    }

                    // the dot in ".<||<" is necessary to shut-up scalariform...
                    ps.scheduleForCollected[Node] { case n @ `nodeRoot` ⇒ n.asInstanceOf[Node] }(
                        analysis(0)
                    )

                    ps.waitOnPropertyComputationCompletion(true)

                    try {
                        ps(nodeRoot, TreeLevelKey) should be(EP(nodeRoot, TreeLevel(0)))
                        ps(nodeRRoot, TreeLevelKey) should be(EP(nodeRRoot, TreeLevel(1)))
                        ps(nodeRRRoot, TreeLevelKey) should be(EP(nodeRRRoot, TreeLevel(2)))
                        ps(nodeLRRoot, TreeLevelKey) should be(EP(nodeLRRoot, TreeLevel(2)))
                        ps(nodeLRoot, TreeLevelKey) should be(EP(nodeLRoot, TreeLevel(1)))
                        ps(nodeLLRoot, TreeLevelKey) should be(EP(nodeLLRoot, TreeLevel(2)))

                    } catch {
                        case t: Throwable ⇒
                            info(s"test failed on run $runs\n"+ps.toString(true))
                            try { ps.validate(None) } catch {
                                case ae: AssertionError ⇒
                                    info(s"validation failed on run $runs\n"+ae.getMessage.toString)
                            }
                            throw t
                    }

                    ps.reset()
                }
                info(s"executed the test $runs times")
            }

            it("should be possible to execute an analysis which at some point stabilizes itself in an intermediate result") {
                import scala.collection.mutable

                val testSizes = Set(1, 5, 50000)
                for (testSize ← testSizes) {
                    // 1. we create a ((very) long) chain
                    val firstNode = Node(0.toString)
                    val allNodes = mutable.Set(firstNode)
                    var prevNode = firstNode
                    for { i ← 1 to 5 } {
                        val nextNode = Node(i.toString)
                        allNodes += nextNode
                        prevNode.targets += nextNode
                        prevNode = nextNode
                    }
                    prevNode.targets += firstNode

                    // 2. we create the store
                    val store = LockBasedPropertyStore(allNodes, () ⇒ false)(GlobalLogContext)

                    def c(node: Node) =
                        new OnUpdateContinuation { c ⇒
                            def apply(
                                e: Entity, p: Property, ut: UpdateType
                            ): PropertyComputationResult = {
                                purityAnalysis(node)
                            }
                        }
                    def purityAnalysis(node: Node): PropertyComputationResult = {
                        val nextNode = node.targets.head // HERE: we always have only one successor
                        store(nextNode, PurityKey) match {
                            case epk: EPK[_, _] ⇒
                                IntermediateResult(node, ConditionallyPure, Iterable(epk), c(node))
                            case EP(_, Pure)   ⇒ Result(node, Pure)
                            case EP(_, Impure) ⇒ Result(node, Impure)
                            case ep @ EP(_, ConditionallyPure) ⇒
                                IntermediateResult(node, ConditionallyPure, Iterable(ep), c(node))
                        }
                    }
                    // 4. execute analysis
                    store.scheduleForCollected { case n: Node ⇒ n }(purityAnalysis)
                    store.waitOnPropertyComputationCompletion(true, true)

                    // 5. let's evaluate the result
                    store.entities(PurityKey) foreach { ep ⇒
                        if (ep.p != Pure) {
                            info(store.toString(true))
                            fail(s"Node(${ep.e}) is not Pure (${ep.p})")
                        }
                    }

                    info(s"test succeeded with $testSize node(s) in a circle")
                }
            }

            it("should be triggered whenever the property is updated and supports an incremental update based on the given property") {
                import scala.collection.mutable
                val ps = psNodes

                var runs = 0
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < TestDuration * 60 * 1000) {
                    runs += 1
                    /* The following analysis only uses the new information given to it and updates
                     * the set of observed dependees.
                     */
                    def analysis(n: Node): PropertyComputationResult = {
                        val nTargets = n.targets
                        if (nTargets.isEmpty)
                            return Result(n, NoReachableNodes);

                        val allDependees: mutable.Set[Node] = nTargets.clone - n // self-dependencies are ignored!
                        var dependeePs: Traversable[EOptionP[Entity, _ <: ReachableNodes]] = ps(allDependees, ReachableNodesKey)

                        // incremental computation
                        def c(
                            dependeeE:  Entity,
                            dependeeP:  Property,
                            updateType: UpdateType
                        ): PropertyComputationResult = {
                            // Get the set of currently reachable nodes:
                            val alreadyReachableNodes: SomeSet[Node] =
                                ps(n, ReachableNodesKey) match {
                                    case EP(_, ReachableNodes(reachableNodes)) ⇒ reachableNodes
                                    case _                                     ⇒ Set.empty
                                }
                            // Get the set of nodes reached by the dependee:
                            val ReachableNodes(depeendeeReachableNodes) = dependeeP

                            // Compute the new set of reachable nodes:
                            val newReachableNodes = alreadyReachableNodes ++ depeendeeReachableNodes
                            val newP = ReachableNodes(newReachableNodes)

                            // Adapt the set of dependeePs to ensure termination
                            dependeePs = dependeePs.filter { _.e ne dependeeE }
                            if (updateType != FinalUpdate) {
                                dependeePs = dependeePs ++ Traversable(EP(dependeeE, dependeeP.asInstanceOf[ReachableNodes]))
                            }
                            if (dependeePs.nonEmpty)
                                IntermediateResult(n, newP, dependeePs, c)
                            else
                                Result(n, newP)
                        }

                        // initial computation
                        val reachableNodes =
                            dependeePs.foldLeft(allDependees.clone) { (reachableNodes, dependee) ⇒
                                if (dependee.hasProperty)
                                    reachableNodes ++ dependee.p.nodes
                                else
                                    reachableNodes
                            }
                        val intermediateP = ReachableNodes(
                            if (n.targets contains n)
                                reachableNodes + n
                            else
                                reachableNodes
                        )
                        IntermediateResult(n, intermediateP, dependeePs, c)
                    }

                    ps.scheduleForCollected { case n: Node ⇒ n }(analysis)
                    ps.waitOnPropertyComputationCompletion(false)

                    try {
                        // the graph:
                        // a -> b -> c
                        //      b -> d
                        //           d ⟲
                        //           d -> e
                        //                e -> r
                        //       ↖︎----------< r
                        ps(nodeA, ReachableNodesKey) should be(EP(nodeA, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeB, ReachableNodesKey) should be(EP(nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeC, ReachableNodesKey) should be(EP(nodeC, ReachableNodes(Set())))
                        ps(nodeC, ReachableNodesKey).isPropertyFinal should be(true)
                        ps(nodeD, ReachableNodesKey) should be(EP(nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeE, ReachableNodesKey) should be(EP(nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeR, ReachableNodesKey) should be(EP(nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                    } catch {
                        case t: Throwable ⇒
                            info(s"test failed on run $runs\n"+ps.toString(true))
                            try { ps.validate(None) } catch {
                                case ae: AssertionError ⇒
                                    info(s"validation failed on run $runs\n"+ae.getMessage)
                            }
                            throw t
                    }

                    ps.reset()
                }
                info(s"executed the test $runs times")
            }

            it("should be triggered whenever the property is updated and supports a complete update based on a query of the store's value") {
                import scala.collection.mutable
                val ps = psNodes

                var runs = 0
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < TestDuration * 60 * 1000) {
                    runs += 1

                    /* The following analysis collects all nodes a node is connected with (i.e.,
                 * it computes the transitive closure).
                 */
                    def analysis(n: Node): PropertyComputationResult = {
                        val nTargets = n.targets
                        if (nTargets.isEmpty)
                            return Result(n, NoReachableNodes);

                        val remainingDependees: mutable.Set[Node] = nTargets.clone - n
                        val c = new Function3[Entity, Property, UpdateType, PropertyComputationResult] {
                            def apply(
                                dependeeE:  Entity,
                                dependeeP:  Property,
                                updateType: UpdateType
                            ): PropertyComputationResult = {
                                // Get the set of currently reachable nodes.
                                val alreadyReachableNodes: SomeSet[Node] =
                                    ps(n, ReachableNodesKey) match {
                                        case EP(_, ReachableNodes(reachableNodes)) ⇒ reachableNodes
                                        case _                                     ⇒ Set.empty
                                    }
                                // Whenever we continue a computation we have have to query
                                // all relevant entities about their "current" properties.
                                // This is strictly necessary to ensure termination because the
                                // property store uses this information to decide whether to immediately
                                // continue the computation or not.
                                val dependeePs = ps[Node, ReachableNodes](remainingDependees, ReachableNodesKey)
                                val dependeesReachableNodes =
                                    dependeePs.foldLeft(remainingDependees.clone) { (reachableNodes, dependee) ⇒
                                        if (dependee.hasProperty)
                                            reachableNodes ++ dependee.p.nodes
                                        else
                                            reachableNodes
                                    }

                                val newReachableNodes = alreadyReachableNodes ++ dependeesReachableNodes
                                val newP = ReachableNodes(newReachableNodes)

                                if (updateType == FinalUpdate) {
                                    remainingDependees -= dependeeE.asInstanceOf[Node]
                                    val filteredDependeePs = dependeePs.filter {
                                        _.e ne dependeeE
                                    }
                                    if (filteredDependeePs.nonEmpty)
                                        IntermediateResult(n, newP, filteredDependeePs, this)
                                    else
                                        Result(n, newP)
                                } else {
                                    IntermediateResult(n, newP, dependeePs, this)
                                }
                            }
                        }

                        // initial computation
                        val dependeePs = ps[Node, ReachableNodes](remainingDependees, ReachableNodesKey)
                        val reachableNodes =
                            dependeePs.foldLeft(remainingDependees.clone) { (reachableNodes, dependee) ⇒
                                if (dependee.hasProperty)
                                    reachableNodes ++ dependee.p.nodes
                                else
                                    reachableNodes
                            }
                        val intermediateP = ReachableNodes(
                            if (n.targets contains n)
                                reachableNodes + n
                            else
                                reachableNodes
                        )
                        IntermediateResult(n, intermediateP, dependeePs, c)
                    }

                    ps.scheduleForCollected { case n: Node ⇒ n }(analysis)
                    ps.waitOnPropertyComputationCompletion(false)

                    try {
                        // the graph:
                        // a -> b -> c
                        //      b -> d
                        //           d ⟲
                        //           d -> e
                        //                e -> r
                        //       ↖︎----------< r
                        ps(nodeA, ReachableNodesKey) should be(EP(nodeA, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeB, ReachableNodesKey) should be(EP(nodeB, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeC, ReachableNodesKey) should be(EP(nodeC, ReachableNodes(Set())))
                        ps(nodeD, ReachableNodesKey) should be(EP(nodeD, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeE, ReachableNodesKey) should be(EP(nodeE, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                        ps(nodeR, ReachableNodesKey) should be(EP(nodeR, ReachableNodes(Set(nodeB, nodeC, nodeD, nodeE, nodeR))))
                    } catch {
                        case t: Throwable ⇒
                            info(s"test failed on run $runs\n"+ps.toString(true))
                            try { ps.validate(None) } catch {
                                case ae: AssertionError ⇒
                                    info(s"validation failed on run $runs\n"+ae.getMessage.toString)
                            }
                            throw t
                    }

                    psNodes.reset()
                }
                info(s"executed the test $runs times")
            }

        }

        describe("lazy computations of an entity's property") {

            it("should not be executed immediately") {
                val ps = psStrings
                @volatile var stringLengthTriggered = false
                val stringLengthPC: PropertyComputation[String] = (e: String) ⇒ {
                    stringLengthTriggered = true
                    Result(e, StringLength(e.size))
                }
                ps scheduleLazyPropertyComputation (StringLengthKey, stringLengthPC)

                if (stringLengthTriggered) fail("computation is already triggered")
                ps.waitOnPropertyComputationCompletion(true)
                if (stringLengthTriggered) fail("computation is already triggered")
            }

            it("should be executed (at most once) when the property is requested") {
                val ps = psStrings
                @volatile var stringLengthTriggered = false
                val stringLengthPC: PropertyComputation[String] = (e: String) ⇒ {
                    if (stringLengthTriggered) fail("computation is already triggered")
                    stringLengthTriggered = true
                    Result(e, StringLength(e.size))
                }
                ps scheduleLazyPropertyComputation (StringLengthKey, stringLengthPC)

                val palindromePC: PropertyComputation[String] = (e: String) ⇒ {
                    val s = e.toString
                    Result(e, if (s.reverse == s) Palindrome else NoPalindrome)
                }
                ps scheduleLazyPropertyComputation (PalindromeKey, palindromePC)

                stringLengthTriggered should be(false)
                ps.waitOnPropertyComputationCompletion(true)

                ps.properties("a") should be(Nil)

                ps("a", StringLengthKey) should be(EPK("a", StringLengthKey)) // this should trigger the computation
                ps("a", StringLengthKey) // but hopefully only once (tested using "triggered")
                ps("a", PalindromeKey)
                ps("a", PalindromeKey)
                ps.waitOnPropertyComputationCompletion(true)

                ps("a", StringLengthKey).p should be(StringLength(1))
                ps("a", PalindromeKey).p should be(Palindrome)
            }
        }

    }
}
