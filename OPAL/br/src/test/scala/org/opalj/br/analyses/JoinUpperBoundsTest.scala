/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.collection.immutable.UIDSet
import scala.language.implicitConversions
import org.opalj.log.GlobalLogContext

/**
 * @author Tobias Becker
 */
@RunWith(classOf[JUnitRunner])
class JoinUpperBoundsTest extends AnyFunSpec with Matchers {

    val classhierachy =
        ClassHierarchy(
            Iterable.empty,
            List(() => this.getClass.getResourceAsStream("ClassHierarchyUpperBounds.ths"))
        )(GlobalLogContext)

    implicit def stringToUIDSetObjectType(str: String) = UIDSet(ObjectType(str))

    implicit def setToUIDSet(s: Set[String]): UIDSet[ObjectType] = {
        UIDSet.fromSpecific(s.map(ObjectType.apply))
    }

    def testJoinUpperTypeBounds(
        param1:    UIDSet[ObjectType],
        param2:    UIDSet[ObjectType],
        reflexive: Boolean,
        expected:  UIDSet[ObjectType]
    ) = {
        // should always be the same value if parameters are swapped
        def mkString(param: UIDSet[ObjectType]) = {
            param.toSeq.map(_.toJava).mkString("{", ",", "}")
        }
        val result1_2 = classhierachy.joinUpperTypeBounds(param1, param2, reflexive)
        if (result1_2 != expected) {
            fail(
                s"${mkString(param1)} join${if (reflexive) "(reflexive)" else ""}"+
                    s" ${mkString(param2)} is ${mkString(result1_2)};"+
                    s" expected ${mkString(expected)}"
            )
        }

        val result2_1 = classhierachy.joinUpperTypeBounds(param2, param1, reflexive)
        if (result2_1 != expected) {
            fail(s"${mkString(param2)} join${if (reflexive) "(reflexive)" else ""}"+
                s" ${mkString(param1)} is ${mkString(result2_1)};"+
                s" expected ${mkString(expected)}")
        }

    }

    describe("the behavior of the method joinUpperTypeBounds of ClassHierachy") {
        // uncomment to display the test graph:
        // io.writeAndOpen((toDot.generateDot(Set(classhierachy.toGraph))), "test", ".dot")

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

                it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                    testJoinUpperTypeBounds("E", "SubE", true, "E")
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

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubSubID", "SubSubIDSubIA", true, "SubID")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubID", "SubSubIDSubIA", false, "SubID")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinUpperTypeBounds("SubSubIDSubIA", "SubSubIDSubIA2", true, Set("SubID", "IA"))
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubIDSubIA", "SubSubIDSubIA2", false, Set("SubID", "IA"))
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubID2", "SubSubID", true, "ID")
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubID2", "SubSubID", false, "ID")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinUpperTypeBounds("SubIDSubIA", "SubSubIDSubIA", true, Set("ID", "IA"))
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinUpperTypeBounds("SubIDSubIA", "SubSubIDSubIA", false, Set("ID", "IA"))
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("SubIDSubIA", "SubIB", true, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("SubIDSubIA", "SubIB", false, "java/lang/Object")
                }

                it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                    testJoinUpperTypeBounds("SubSubID2", "SubID2", true, "SubID2")
                }
            }

            describe("the behavior of joins with sets containing more than one interface") {
                it("join a set of interfaces with itself should result in the same set") {
                    testJoinUpperTypeBounds(Set("IA", "IB", "IC"), Set("IA", "IB", "IC"), true, Set("IA", "IB", "IC"))
                }

                it("join of a interface and several direct subinterfaces should result in the interface") {
                    testJoinUpperTypeBounds("ID", Set("SubSubID", "SubSubID2"), true, "ID")
                }

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
                    testJoinUpperTypeBounds("SubSubID", Set("SubID2", "SubID3"), true, "ID")
                    testJoinUpperTypeBounds(Set("SubSubID", "SubID2"), Set("SubID", "SubID3"), true, "SubID")
                    testJoinUpperTypeBounds(Set("SubSubID", "SubID3"), Set("SubID", "SubID2"), true, "SubID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds("SubSubID", Set("SubID2", "SubID3"), false, "ID")
                    testJoinUpperTypeBounds("SubID3", Set("SubSubID", "SubID2"), false, "ID")
                    testJoinUpperTypeBounds("SubID2", Set("SubSubID", "SubID3"), false, "ID")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds(Set("ID", "IA", "IC"), "SubIB", true, "java/lang/Object")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("ID", "IA", "IC"), "SubIB", false, "java/lang/Object")
                }

                it("join of several interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubID3", "SubID2"), "SubSubIDSubIA", true, "ID")
                }

                it("join of several interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("SubID3", "SubID2"), "SubSubIDSubIA", false, "ID")
                }

                it("join of several interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubID2", "SubSubIDSubIA"), Set("SubID3", "SubSubIDSubIA2"), true, Set("SubID", "IA"))
                }

                it("join of several interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("SubID2", "SubSubIDSubIA"), Set("SubID3", "SubSubIDSubIA2"), false, Set("SubID", "IA"))
                }

                it("join of several interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubSubID2", "SubSubIDSubIA"), Set("SubID3", "SubIB"), true, "ID")
                }

                it("join of several interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("SubSubID2", "SubSubIDSubIA"), Set("SubID3", "SubIB"), false, "ID")
                }

                it("join of several interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubSubID2", "SubSubIDSubIA"), Set("SubID3", "SubIDSubIA"), true, Set("ID", "IA"))
                }

                it("join of several interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinUpperTypeBounds(Set("SubSubID2", "SubSubIDSubIA"), Set("SubID3", "SubIDSubIA"), false, Set("ID", "IA"))
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("SubIB", Set("SubID3", "IA"), true, "java/lang/Object")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("SubIB", Set("SubID3", "IA"), false, "java/lang/Object")
                }

                it("join of interface and several subinterfaces with re-implemented interface should result in the interface (reflexive)") {
                    testJoinUpperTypeBounds(Set("SubSubID2", "SubSubID22"), "SubID2", true, "SubID2")
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

                it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                    testJoinUpperTypeBounds("D", "IA", true, "IA")
                }

                it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                    testJoinUpperTypeBounds("SubSubA", "SubID3", true, "SubID3")
                }

                it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                    testJoinUpperTypeBounds("SubB", "SubIB", true, "SubIB")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("C", "IA", true, "java/lang/Object")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (non-reflexive)") {
                    testJoinUpperTypeBounds("C", "IA", false, "java/lang/Object")
                }
            }
            describe("the behavior of joins with sets containing one interfaces and one class") {

                it("join of class and superclass and superinterface should result in both (reflexive)") {
                    testJoinUpperTypeBounds("SubB", Set("SubIB", "B"), true, Set("SubIB", "B"))
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinUpperTypeBounds("B", Set("SubE", "IA"), true, "java/lang/Object")
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (nonreflexive)") {
                    testJoinUpperTypeBounds("B", Set("SubE", "IA"), false, "java/lang/Object")
                }
            }
        }
    }
}
