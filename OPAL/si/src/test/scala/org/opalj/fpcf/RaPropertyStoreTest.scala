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
//package org.opalj.fpcf
//
//import org.junit.runner.RunWith
//import org.opalj.fpcf.TestProperties.{IntValue, StringLength, TreeLevel}
//import org.opalj.log.GlobalLogContext
//import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
//import org.scalatest.junit.JUnitRunner
//
//import scala.collection.mutable
//
///**
// * Tests the ReactiveAsyncPropertyStore
// *
// * @author Andreas Muttscheller
// */
//class ReactiveAsyncPropertyStoreTest extends PropertyStoreTest {
//    implicit val logContext = GlobalLogContext
//
//    override def buildPropertyStore(): PropertyStore = {
//        ReactiveAsyncPropertyStore(
//            entities,
//            () ⇒ false
//        )
//    }
//}
//
////**********************************************************************************************
////
//// PROPERTIES
//object TestProperties {
//    final val IntPropertyKey: PropertyKey[IntValue] = {
//        PropertyKey.create(
//            "IntProperty",
//            (ps: PropertyStore, e: Entity) ⇒ IntValue(-1),
//            (ps: PropertyStore, eps: SomeEPS) ⇒ eps.toUBEP
//        )
//    }
//    case class IntValue(i: Int) extends Property {
//        final type Self = IntValue
//        final def key = IntPropertyKey
//        final def isRefinable: Boolean = true
//    }
//
//    final val StringLengthKey: PropertyKey[StringLength] = {
//        PropertyKey.create(
//            "StringLength",
//            (ps: PropertyStore, e: Entity) ⇒ ???,
//            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
//        )
//    }
//    case class StringLength(length: Int) extends Property {
//        final type Self = StringLength
//        final def key = StringLengthKey
//        final def isRefinable: Boolean = false
//    }
//
//    class Node(val name: String, val targets: mutable.Set[Node] = mutable.Set.empty) {
//        override def hashCode: Int = name.hashCode()
//        override def equals(other: Any): Boolean = other match {
//            case that: Node ⇒ this.name equals that.name
//            case _          ⇒ false
//        }
//        //override def toString: String = s"""$name->{${targets.map(_.name).mkString(",")}}"""
//        override def toString: String = name
//    }
//    object Node { def apply(name: String) = new Node(name) }
//
//    // DESCRIPTION OF A TREE
//    val nodeRoot = Node("Root")
//    val nodeLRoot = Node("Root->L")
//    val nodeLLRoot = Node("Root->L->L")
//    val nodeRRoot = Node("Root->R")
//    val nodeLRRoot = Node("Root->R->L")
//    val nodeRRRoot = Node("Root->R->R")
//    nodeRoot.targets += nodeLRoot
//    nodeRoot.targets += nodeRRoot
//    nodeLRoot.targets += nodeLLRoot
//    nodeRRoot.targets += nodeLRRoot
//    nodeRRoot.targets += nodeRRRoot
//    val treeEntities = List[Node](nodeRoot, nodeLRoot, nodeLLRoot, nodeRRoot, nodeLRRoot, nodeRRRoot)
//
//    final val TreeLevelKey: PropertyKey[TreeLevel] = {
//        PropertyKey.create(
//            "TreeLevel",
//            (ps: PropertyStore, e: Entity) ⇒ ???,
//            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ ???
//        )
//    }
//    case class TreeLevel(length: Int) extends Property {
//        final type Self = TreeLevel
//        final def key = TreeLevelKey
//        final def isRefinable = false
//    }
//}
//
//@RunWith(classOf[JUnitRunner])
//trait PropertyStoreTest extends FunSpec with Matchers with BeforeAndAfterEach {
//
//    var ps: PropertyStore = null
//
//    def buildPropertyStore(): PropertyStore
//
//    override def beforeEach(): Unit = {
//        ps = buildPropertyStore()
//        super.beforeEach()
//    }
//
//    val entities: Traversable[Entity] = Traversable("foo", "bar", "baz") ++ TestProperties.treeEntities
//
//    //**********************************************************************************************
//    //
//    // TESTS
//
//    describe("Simple set and get operations") {
//        it("should return EPK for unknown values") {
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EPK("foo", TestProperties.IntPropertyKey) ⇒ }
//        }
//
//        it("should set values properly") {
//            ps.set("foo", IntValue(1))
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//            ps(EPK("foo", TestProperties.IntPropertyKey)) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//        }
//
//        it("should not be possible to set the same value twice") {
//            ps.set("foo", IntValue(1))
//            assertThrows[IllegalArgumentException] {
//                ps.set("foo", IntValue(2))
//            }
//        }
//
//        it("should eagerly schedule and compute values properly") {
//            ps.scheduleForEntity("foo")(e ⇒ Result(e, IntValue(1)))
//            ps.waitOnPropertyComputationCompletion(false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//        }
//
//        it("should lazily schedule and compute values properly") {
//            val computation: SomePropertyComputation = {
//                e: Entity ⇒ Result(e, IntValue(1))
//            }
//
//            ps.scheduleLazyPropertyComputation(TestProperties.IntPropertyKey, computation)
//            ps.waitOnPropertyComputationCompletion(false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EPK("foo", TestProperties.IntPropertyKey) ⇒ }
//
//            ps.waitOnPropertyComputationCompletion(false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//        }
//
//        it("should be able to handle RefinableResult") {
//            ps.scheduleForEntity("foo")(e ⇒ RefinableResult(e, IntValue(1)))
//            ps.waitOnPropertyComputationCompletion(false)
//
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//            ps("foo", TestProperties.IntPropertyKey).isPropertyFinal should be(false)
//
//            ps.scheduleForEntity("bar")(_ ⇒ MultiResult(List(EP("foo", IntValue(2)), EP("bar", IntValue(1)))))
//            ps.waitOnPropertyComputationCompletion(false)
//
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(2)) ⇒ }
//            ps("foo", TestProperties.IntPropertyKey).isPropertyFinal should be(true)
//        }
//
//        it("should be able to handle Results") {
//            ps.scheduleForEntity("bar")(_ ⇒ Results(List(Result("foo", IntValue(1)), Result("bar", IntValue(1)))))
//            ps.waitOnPropertyComputationCompletion(false)
//
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(1)) ⇒ }
//
//            ps.entities(TestProperties.IntPropertyKey) should contain allOf (EP("foo", IntValue(1)), EP("bar", IntValue(1)))
//
//            ps("foo", TestProperties.IntPropertyKey).isPropertyFinal should be(true)
//            ps("bar", TestProperties.IntPropertyKey).isPropertyFinal should be(true)
//        }
//
//        it("should be able to set MultiResult") {
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(1),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                (_, _, _) ⇒ NoResult
//            ))
//            ps.waitOnPropertyComputationCompletion(false)
//
//            ps.scheduleForEntity("bar")(_ ⇒ MultiResult(List(EP("foo", IntValue(1)), EP("bar", IntValue(1)))))
//            ps.waitOnPropertyComputationCompletion(false)
//
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(1)) ⇒ }
//
//            ps.entities(TestProperties.IntPropertyKey) should contain allOf (EP("foo", IntValue(1)), EP("bar", IntValue(1)))
//        }
//
//        it("should be possible to execute an analysis incrementally") {
//            def analysis(level: Int)(n: TestProperties.Node): PropertyComputationResult = {
//                val nextPCs: Traversable[(PropertyComputation[TestProperties.Node], TestProperties.Node)] =
//                    n.targets.map(t ⇒ (analysis(level + 1) _, t))
//                IncrementalResult(Result(n, TreeLevel(level)), nextPCs)
//            }
//
//            ps.scheduleForEntity(TestProperties.nodeRoot)(analysis(0))
//
//            ps.waitOnPropertyComputationCompletion(true)
//
//            ps(TestProperties.nodeRoot, TestProperties.TreeLevelKey) should be(EP(TestProperties.nodeRoot, TreeLevel(0)))
//            ps(TestProperties.nodeRRoot, TestProperties.TreeLevelKey) should be(EP(TestProperties.nodeRRoot, TreeLevel(1)))
//            ps(TestProperties.nodeRRRoot, TestProperties.TreeLevelKey) should be(EP(TestProperties.nodeRRRoot, TreeLevel(2)))
//            ps(TestProperties.nodeLRRoot, TestProperties.TreeLevelKey) should be(EP(TestProperties.nodeLRRoot, TreeLevel(2)))
//            ps(TestProperties.nodeLRoot, TestProperties.TreeLevelKey) should be(EP(TestProperties.nodeLRoot, TreeLevel(1)))
//            ps(TestProperties.nodeLLRoot, TestProperties.TreeLevelKey) should be(EP(TestProperties.nodeLLRoot, TreeLevel(2)))
//        }
//    }
//
//    describe("Computations with simple dependencies and fallbacks") {
//        it("should compute simple dependencies correctly") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                if (e == "bar") {
//                    Result("foo", IntValue(p.asInstanceOf[IntValue].i * 2))
//                } else {
//                    NoResult
//                }
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(1),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.scheduleForEntity("bar")(e ⇒ Result(e, IntValue(5)))
//            ps.waitOnPropertyComputationCompletion(true, true)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(10)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(5)) ⇒ }
//        }
//
//        it("should compute simple dependencies with fallback correctly") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                if (e == "bar") {
//                    Result("foo", IntValue(p.asInstanceOf[IntValue].i * 2))
//                } else {
//                    NoResult
//                }
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(1),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.waitOnPropertyComputationCompletion(true, true)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(-2)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(-1)) ⇒ }
//        }
//
//        it("should not compute fallback if not set") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                if (e == "bar") {
//                    Result("foo", IntValue(p.asInstanceOf[IntValue].i * 2))
//                } else {
//                    NoResult
//                }
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(1),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EPK("bar", TestProperties.IntPropertyKey) ⇒ }
//        }
//
//        it("should be able to set a value if the cell exists due to dependencies") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                NoResult
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps.set("bar", IntValue(1))
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern {
//                case EP("foo", IntValue(-42))                  ⇒
//                case EPK("foo", TestProperties.IntPropertyKey) ⇒ // If debug consistency is on
//            }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(1)) ⇒ }
//        }
//
//        it("should call c if the value for the entity was previously computed") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                Result("foo", IntValue(2))
//            }
//            ps.scheduleForEntity("bar")(e ⇒ Result(
//                e,
//                IntValue(1)
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(1),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(2)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(1)) ⇒ }
//        }
//
//        it("should handle EP dependencies correctly") {
//            val i1 = IntValue(1)
//            ps.scheduleForEntity("bar")(e ⇒ IntermediateResult(
//                e,
//                i1,
//                Set(EPK("baz", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        Result("bar", p)
//                    }
//                    c(e, p, ut)
//                }
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(1),
//                Set(EP("bar", i1)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        Result("foo", IntValue(42))
//                    }
//                    c(e, p, ut)
//                }
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(1)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(1)) ⇒ }
//
//            ps.handleResult(Result("bar", IntValue(10)))
//
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(42)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(10)) ⇒ }
//        }
//    }
//
//    describe("Cycles resolution") {
//        it("shouldn't resolve a cycle if not set") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                NoResult
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.waitOnPropertyComputationCompletion(false, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern {
//                case EP("foo", IntValue(42))                   ⇒
//                case EPK("foo", TestProperties.IntPropertyKey) ⇒ // If debug consistency is on
//            }
//        }
//
//        it("should resolve a self cycle without cycle resolution") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                NoResult
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern {
//                case EP("foo", IntValue(-42))                  ⇒
//                case EPK("foo", TestProperties.IntPropertyKey) ⇒ // If debug consistency is on
//            }
//        }
//
//        it("should compute a cycle with multiple entities") {
//            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                if (e == "foo") {
//                    Result("bar", IntValue(42))
//                } else {
//                    Result("foo", IntValue(42))
//                }
//            }
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.scheduleForEntity("bar")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                c
//            ))
//            ps.scheduleForEntity("baz")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        if (ut.isFinalUpdate) {
//                            Result("baz", p)
//                        } else {
//                            IntermediateResult("baz", IntValue(42), Set(EP("foo", p)), c)
//                        }
//                    }
//                    c(e, p, ut)
//                }
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(42)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(42)) ⇒ }
//            ps("baz", TestProperties.IntPropertyKey) should matchPattern { case EP("baz", IntValue(42)) ⇒ }
//        }
//
//        it("should resolve a cycle with multiple entities") {
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        if (ut.isFinalUpdate) {
//                            Result("foo", p)
//                        } else {
//                            IntermediateResult("foo", IntValue(0), Set(EP("bar", p)), c)
//                        }
//                    }
//                    c(e, p, ut)
//                }
//            ))
//            ps.scheduleForEntity("bar")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        if (ut.isFinalUpdate) {
//                            Result("bar", p)
//                        } else {
//                            IntermediateResult("bar", IntValue(0), Set(EP("foo", p)), c)
//                        }
//                    }
//                    c(e, p, ut)
//                }
//            ))
//
//            ps.scheduleForEntity("baz")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        if (ut.isFinalUpdate) {
//                            Result("baz", p)
//                        } else {
//                            IntermediateResult("baz", IntValue(0), Set(EP("foo", p)), c)
//                        }
//                    }
//                    c(e, p, ut)
//                }
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(2)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(2)) ⇒ }
//            ps("baz", TestProperties.IntPropertyKey) should matchPattern { case EP("baz", IntValue(2)) ⇒ }
//        }
//
//        it("should resolve a cycle which depends on a fallback property") {
//            var d1: Set[SomeEOptionP] = Set(EPK("bar", TestProperties.IntPropertyKey), EPK("baz", TestProperties.IntPropertyKey))
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                d1,
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        if (e == "baz") {
//                            Result("foo", IntValue(42))
//                        } else if (ut.isFinalUpdate) {
//                            Result("foo", p)
//                        } else {
//                            d1 = d1.filter { _.e ne e } + EP("bar", p)
//                            IntermediateResult("foo", IntValue(0), d1, c)
//                        }
//                    }
//                    c(e, p, ut)
//                }
//            ))
//            ps.scheduleForEntity("bar")(e ⇒ IntermediateResult(
//                e,
//                IntValue(-42),
//                Set(EPK("foo", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ {
//                    def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
//                        if (ut.isFinalUpdate) {
//                            Result("bar", p)
//                        } else {
//                            IntermediateResult("bar", IntValue(0), Set(EP("foo", p)), c)
//                        }
//                    }
//                    c(e, p, ut)
//                }
//            ))
//
//            ps.waitOnPropertyComputationCompletion(true, true)
//            ps("foo", TestProperties.IntPropertyKey) should matchPattern { case EP("foo", IntValue(42)) ⇒ }
//            ps("bar", TestProperties.IntPropertyKey) should matchPattern { case EP("bar", IntValue(42)) ⇒ }
//            ps("baz", TestProperties.IntPropertyKey) should matchPattern { case EP("baz", IntValue(-1)) ⇒ }
//        }
//    }
//
//    describe("entities and properties methods") {
//        it("should return an empty list for entities without properties") {
//            ps.properties("foo") should be('empty)
//            ps.properties("bar") should be('empty)
//            ps.properties("baz") should be('empty)
//        }
//
//        it("should return properties for an entity with one value") {
//            ps.set("foo", IntValue(42))
//
//            ps.properties("foo") should contain(IntValue(42))
//            ps.properties("bar") should be('empty)
//            ps.properties("baz") should be('empty)
//        }
//
//        it("should return properties for an entity with multiple value") {
//            ps.set("foo", IntValue(42))
//            ps.set("foo", StringLength(1))
//
//            ps.properties("foo") should contain(IntValue(42))
//            ps.properties("foo") should contain(StringLength(1))
//            ps.properties("bar") should be('empty)
//            ps.properties("baz") should be('empty)
//        }
//
//        it("should not return any entities when nothing was computed") {
//            ps.entities(IntValue(1)) should be('empty)
//            ps.entities(TestProperties.IntPropertyKey) should be('empty)
//            ps.entities(_ ⇒ true) should be('empty)
//        }
//
//        it("should return entities when a property was set") {
//            ps.set("foo", IntValue(42))
//            ps.set("bar", IntValue(42))
//            ps.set("bar", StringLength(1))
//
//            ps.entities(IntValue(42)) should contain allOf ("foo", "bar")
//            ps.entities(TestProperties.IntPropertyKey) should contain allOf (EP("foo", IntValue(42)), EP("bar", IntValue(42)))
//            ps.entities(_ ⇒ true) should contain allOf ("foo", "bar")
//
//            ps.entities(StringLength(1)) should contain("bar")
//            ps.entities(StringLength(2)) should be('empty)
//        }
//
//        it("should not return entities for a EP that was not computed, but is known (dependency)") {
//            ps.scheduleForEntity("foo")(e ⇒ IntermediateResult(
//                e,
//                IntValue(42),
//                Set(EPK("bar", TestProperties.IntPropertyKey)),
//                (e, p, ut) ⇒ { NoResult }
//            ))
//            ps.waitOnPropertyComputationCompletion(true, false)
//
//            ps.properties("foo") should contain(IntValue(42))
//            ps.properties("bar") should be('empty)
//
//            ps.entities(IntValue(42)) should contain("foo")
//            ps.entities(TestProperties.IntPropertyKey) should contain(EP("foo", IntValue(42)))
//            ps.entities(_ ⇒ true) should contain("foo")
//
//            ps.entities(StringLength(2)) should be('empty)
//        }
//    }
//}
