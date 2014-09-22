package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.opalj.graphs.toDot
import org.opalj.collection.immutable.UIDSet
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class JoinUpperBoundsTest
        extends FunSpec
        with Matchers {
    val classhierachy = ClassHierarchy(
        Traversable.empty,
        List(() ⇒ this.getClass.getResourceAsStream("ClassHierachyUpperBounds.ths"))
    )

    implicit def stringToUIDSetObjectType(str: String) = UIDSet(ObjectType(str))

    implicit def setToUIDSet(s: Set[String]) = UIDSet(s.map(ObjectType.apply))

    def testJoinUpperTypeBounds(param1: UIDSet[ObjectType], param2: UIDSet[ObjectType], reflexive: Boolean, expected: UIDSet[ObjectType]) = {
        // should always be the same value if parameters are swapped
        def mkString(param: UIDSet[ObjectType]) = {
            param.toSeq.map(_.toJava).mkString("{", ",", "}")
        }
        val result1_2 = classhierachy.joinUpperTypeBounds(param1, param2, reflexive)
        if (result1_2 != expected)
            fail(s"${mkString(param1)} join${if (reflexive) "(reflexive)" else ""} ${mkString(param2)} is ${mkString(result1_2)}; expected ${mkString(expected)}")

        val result2_1 = classhierachy.joinUpperTypeBounds(param2, param1, reflexive)
        if (result2_1 != expected)
            fail(s"${mkString(param2)} join${if (reflexive) "(reflexive)" else ""} ${mkString(param1)} is ${mkString(result2_1)}; expected ${mkString(expected)}")

    }

    describe("the behavior of the method joinUpperTypeBounds of ClassHierachy") {
        // uncomment to display the test graph:
        //util.writeAndOpen((toDot.generateDot(Set(classhierachy.toGraph))), "test", ".dot")

        describe("the behavior of joins with classes") {
            describe("the behavior of joins with sets containing one class") {
                it("join class with itself should result in the same class") {
                    testJoinUpperTypeBounds("A", "A", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (reflexive)") {
                    testJoinUpperTypeBounds("SubA", "SubA2", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                    testJoinUpperTypeBounds("SubA", "SubA2", false, "A")
                }

                it("join of two indirect subclasses should result in the superclass (reflexive)") {
                    testJoinUpperTypeBounds("SubSubA", "SubA2", true, "A")
                }

                it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubA", "SubA2", false, "A")
                }

                it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                    testJoinUpperTypeBounds("A", "SubA", true, "A")
                }

                it("join of a class and a subclass with common implemented interfaces should result in the common implemented interfaces (non-reflexive)") {
                    testJoinUpperTypeBounds("A", "SubA", false, "IA")
                }

                it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                    testJoinUpperTypeBounds("E", "SubE", true, "E")
                }

                it("join of a class and a subclass with no common implemented interfaces should result in the superclass (non-reflexive)") {
                    testJoinUpperTypeBounds("E", "SubE", false, "java/lang/Object")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("E", "C", true, "java/lang/Object")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("E", "C", false, "java/lang/Object")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                    testJoinUpperTypeBounds("SubA2", "SubA3", true, "A")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinUpperTypeBounds("SubA2", "SubA3", false, "A")
                }

                it("join of classes with same direct superclass, same interface should result in their superclass and common interface (reflexive)") {
                    testJoinUpperTypeBounds("SubA", "SubA4", true, Set("A", "ID"))
                }

                it("join of classes with same direct superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubA", "SubA4", false, Set("A", "ID"))
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                    testJoinUpperTypeBounds("SubSubA", "SubSubA2", true, "A")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubA", "SubSubA2", false, "A")
                }

                it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (reflexive)") {
                    testJoinUpperTypeBounds("SubA4", "SubSubA", true, Set("A", "ID"))
                }

                it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubA4", "SubSubA", false, Set("A", "ID"))
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("SubA3", "SubB", true, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("SubA3", "SubB", false, "java/lang/Object")
                }

                it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                    testJoinUpperTypeBounds("SubB", "SubC", true, "SubIB")
                }

                it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubB", "SubC", false, "SubIB")
                }

                it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                    testJoinUpperTypeBounds("SubA", "SubSubA", true, "SubA")
                }

                it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubB", "SubB2", true, Set("B", "SubIB"))
                }

                it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubB", "SubB2", false, Set("B", "SubIB"))
                }
            }

            describe("the behavior of joins with sets containing more than one class") {
                it("join a set of classes with itself should result in the same set") {
                    testJoinUpperTypeBounds(Set("A", "B"), Set("A", "B"), true, Set("A", "B"))
                }

                it("join of several direct subclasses should result in the superclass (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubA2", "SubA"), "SubA3", true, "A")
                    testJoinUpperTypeBounds(Set("SubA", "SubA3"), "SubA2", true, "A")
                    testJoinUpperTypeBounds(Set("SubA3", "SubA2"), "SubA", true, "A")
                }

                it("join of several direct subclasses should result in the superclass (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("SubA2", "SubA"), "SubA3", false, "A")
                    testJoinUpperTypeBounds(Set("SubA", "SubA3"), "SubA2", false, "A")
                    testJoinUpperTypeBounds(Set("SubA3", "SubA2"), "SubA", false, "A")
                }
            }
        }

        describe("the behavior of joins with interfaces") {
            describe("the behavior of joins with sets containing one interfaces") {
                it("join interface with itself should result in the same interface") {
                    testJoinUpperTypeBounds("SubIB", "SubIB", true, "SubIB")
                }

                it("join of interface and its direct superinterface should result in the interface") {
                    testJoinUpperTypeBounds("ID", "SubID", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubID", "SubID2", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubID", "SubID2", false, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubSubID", "SubID2", true, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubID", "SubID2", false, "ID")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("IE", "SubID", true, "java/lang/Object")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("IE", "SubID", false, "java/lang/Object")
                }
            }

            describe("the behavior of joins with sets containing more than one interface") {

                it("join of several direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubID2", "SubID"), "SubID3", true, "ID")
                    testJoinUpperTypeBounds(Set("SubID", "SubID3"), "SubID2", true, "ID")
                    testJoinUpperTypeBounds(Set("SubID3", "SubID2"), "SubID", true, "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("SubID2", "SubID"), "SubID3", false, "ID")
                    testJoinUpperTypeBounds(Set("SubID", "SubID3"), "SubID2", false, "ID")
                    testJoinUpperTypeBounds(Set("SubID3", "SubID2"), "SubID", false, "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (reflexive)") {
                    //     testJoinUpperTypeBounds("SubSubID", Set("SubID2", "SubID3"), true, "ID")
                    testJoinUpperTypeBounds(Set("SubSubID", "SubID2"), Set("SubID", "SubID3"), true, "ID")
                    //    testJoinUpperTypeBounds(Set("SubSubID", "SubID3"), Set("SubID", "SubID2"), true, "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubID", Set("SubID2", "SubID3"), false, "ID")
                    testJoinUpperTypeBounds(Set("SubSubID", "SubID2"), Set("SubID", "SubID3"), false, "ID")
                    testJoinUpperTypeBounds(Set("SubSubID", "SubID3"), Set("SubID", "SubID2"), false, "ID")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("SubIB", Set("SubID3", "IA"), true, "java/lang/Object")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("SubIB", Set("SubID3", "IA"), false, "java/lang/Object")
                }
            }
        }

        describe("the behavior of joins with classes and interfaces") {
            describe("the behavior of joins with sets containing one interfaces or class") {
                it("join of class and its only interface should result in the interface (reflexive)") {
                    testJoinUpperTypeBounds("A", "IA", true, "IA")
                }

                it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubA", "ID", true, "ID")
                }

            }

        }
    }
}