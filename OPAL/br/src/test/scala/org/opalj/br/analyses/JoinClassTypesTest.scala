/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.collection.immutable.UIDSet
import org.opalj.log.GlobalLogContext

/**
 * @author Tobias Becker
 */
@RunWith(classOf[JUnitRunner])
class JoinClassTypesTest extends AnyFunSpec with Matchers {

    final val classhierarchy = {
        // val thisClass = classOf[org.opalj.br.analyses.JoinClassTypesTest]
        val thisClass = this.getClass
        val in = thisClass.getResourceAsStream("ClassHierarchyUpperBounds.ths")
        if (in == null)
            throw new UnknownError("class hierarchy could not be loaded")
        ClassHierarchy(
            Iterable.empty,
            List(() => in)
        )(using GlobalLogContext)
    }

    implicit def stringToUIDSetClassType(str: String): UIDSet[ClassType] = UIDSet(ClassType(str))

    implicit def stringToClassType(str: String): ClassType = ClassType(str)

    implicit def setToUIDSet(s: Set[String]): UIDSet[ClassType] = {
        UIDSet.empty[ClassType] ++ s.map(ClassType.apply)
    }

    def mkString(param: UIDSet[ClassType]) = {
        param.toSeq.map(_.toJava).mkString("{", ",", "}")
    }

    def testJoinOfTwoClassTypes(
        param1:    ClassType,
        param2:    ClassType,
        reflexive: Boolean,
        expected:  UIDSet[ClassType]
    ) = {
        val result = classhierarchy.joinClassTypes(param1, param2, reflexive)
        if (result != expected)
            fail(
                s"$param1 join${if (reflexive) "(reflexive)" else ""}" +
                    s" $param2 is ${mkString(result)};" +
                    s" expected ${mkString(expected)}"
            )
    }

    def testJoinOfClassTypesWithUpperBound(
        param1:    ClassType,
        param2:    UIDSet[ClassType],
        reflexive: Boolean,
        expected:  UIDSet[ClassType]
    ) = {
        val result = classhierarchy.joinClassTypes(param1, param2, reflexive)
        if (result != expected)
            fail(
                s"$param1 join${if (reflexive) "(reflexive)" else ""}" +
                    s" ${mkString(param2)} is ${mkString(result)};" +
                    s" expected ${mkString(expected)}"
            )
    }

    def testJoinClassTypesUntilSingleUpperBound(
        param1:    ClassType,
        param2:    ClassType,
        reflexive: Boolean,
        expected:  ClassType
    ) = {
        val result = classhierarchy.joinClassTypesUntilSingleUpperBound(param1, param2, reflexive)
        if (result != expected)
            fail(s"$param1 join $param2 ${if (reflexive) "(reflexive)" else ""}" +
                s" with joinObjectTypesUntilSingleUpperBound(ClassType, ObjectType, Boolean) is $result;" +
                s" expected $expected")
    }

    def testJoinClassTypesUntilSingleUpperBound(
        param:    UIDSet[ClassType],
        expected: ClassType
    ) = {
        val result = classhierarchy.joinClassTypesUntilSingleUpperBound(param)
        if (result != expected)
            fail(s"join of ${mkString(param)}" +
                s" using joinObjectTypesUntilSingleUpperBound(UIDSet[ClassType]) is $result;" +
                s" expected $expected")

    }

    describe("the behavior of the method joinClassTypes(ClassType,ClassType) of ClassHierarchy") {
        // uncomment to display the test graph:
        // io.writeAndOpen((toDot.generateDot(Set(classhierarchy.toGraph))), "test", ".dot")

        describe("the behavior of joins with classes") {

            it("join class with itself should result in the same class") {
                testJoinOfTwoClassTypes("A", "A", true, "A")
            }

            it("join of two direct subclasses should result in the superclass (reflexive)") {
                testJoinOfTwoClassTypes("SubA", "SubA2", true, "A")
            }

            it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                testJoinOfTwoClassTypes("SubA", "SubA2", false, "A")
            }

            it("join of two indirect subclasses should result in the superclass (reflexive)") {
                testJoinOfTwoClassTypes("SubSubA", "SubA2", true, "A")
            }

            it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                testJoinOfTwoClassTypes("SubSubA", "SubA2", false, "A")
            }

            it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                testJoinOfTwoClassTypes("A", "SubA", true, "A")
            }

            it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                testJoinOfTwoClassTypes("E", "SubE", true, "E")
            }

            it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                testJoinOfTwoClassTypes("E", "C", true, "java/lang/Object")
            }

            it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoClassTypes("E", "C", false, "java/lang/Object")
            }

            it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                testJoinOfTwoClassTypes("SubA2", "SubA3", true, "A")
            }

            it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                testJoinOfTwoClassTypes("SubA2", "SubA3", false, "A")
            }

            it("join of classes with same direct superclass, same interface should result in their superclass and common interface (reflexive)") {
                testJoinOfTwoClassTypes("SubA", "SubA4", true, Set("A", "ID"))
            }

            it("join of classes with same direct superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubA", "SubA4", false, Set("A", "ID"))
            }

            it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                testJoinOfTwoClassTypes("SubSubA", "SubSubA2", true, "A")
            }

            it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                testJoinOfTwoClassTypes("SubSubA", "SubSubA2", false, "A")
            }

            it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (reflexive)") {
                testJoinOfTwoClassTypes("SubA4", "SubSubA", true, Set("A", "ID"))
            }

            it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubA4", "SubSubA", false, Set("A", "ID"))
            }

            it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                testJoinOfTwoClassTypes("SubA3", "SubB", true, "java/lang/Object")
            }

            it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoClassTypes("SubA3", "SubB", false, "java/lang/Object")
            }

            it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                testJoinOfTwoClassTypes("SubB", "SubC", true, "SubIB")
            }

            it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubB", "SubC", false, "SubIB")
            }

            it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                testJoinOfTwoClassTypes("SubA", "SubSubA", true, "SubA")
            }

            it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubB", "SubB2", true, Set("B", "SubIB"))
            }

            it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubB", "SubB2", false, Set("B", "SubIB"))
            }

        }

        describe("the behavior of joins with interfaces") {

            it("join interface with itself should result in the same interface") {
                testJoinOfTwoClassTypes("SubIB", "SubIB", true, "SubIB")
            }

            it("join of interface and its direct superinterface should result in the interface") {
                testJoinOfTwoClassTypes("ID", "SubID", true, "ID")
            }

            it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubID", "SubID2", true, "ID")
            }

            it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubID", "SubID2", false, "ID")
            }

            it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubSubID", "SubID2", true, "ID")
            }

            it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubSubID", "SubID2", false, "ID")
            }

            it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                testJoinOfTwoClassTypes("IE", "SubID", true, "java/lang/Object")
            }

            it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoClassTypes("IE", "SubID", false, "java/lang/Object")
            }

            it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubSubID", "SubSubIDSubIA", true, "SubID")
            }

            it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubSubID", "SubSubIDSubIA", false, "SubID")
            }

            it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                testJoinOfTwoClassTypes("SubSubIDSubIA", "SubSubIDSubIA2", true, Set("SubID", "IA"))
            }

            it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                testJoinOfTwoClassTypes("SubSubIDSubIA", "SubSubIDSubIA2", false, Set("SubID", "IA"))
            }

            it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubID2", "SubSubID", true, "ID")
            }

            it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                testJoinOfTwoClassTypes("SubID2", "SubSubID", false, "ID")
            }

            it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                testJoinOfTwoClassTypes("SubIDSubIA", "SubSubIDSubIA", true, Set("ID", "IA"))
            }

            it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                testJoinOfTwoClassTypes("SubIDSubIA", "SubSubIDSubIA", false, Set("ID", "IA"))
            }

            it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                testJoinOfTwoClassTypes("SubIDSubIA", "SubIB", true, "java/lang/Object")
            }

            it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoClassTypes("SubIDSubIA", "SubIB", false, "java/lang/Object")
            }

            it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                testJoinOfTwoClassTypes("SubSubID2", "SubID2", true, "SubID2")
            }

        }

        describe("the behavior of joins with classes and interfaces") {
            it("join of class and its only interface should result in the interface (reflexive)") {
                testJoinOfTwoClassTypes("A", "IA", true, "IA")
            }

            it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubA", "ID", true, "ID")
            }

            it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                testJoinOfTwoClassTypes("D", "IA", true, "IA")
            }

            it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                testJoinOfTwoClassTypes("SubSubA", "SubID3", true, "SubID3")
            }

            it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                testJoinOfTwoClassTypes("SubB", "SubIB", true, "SubIB")
            }

            it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                testJoinOfTwoClassTypes("C", "IA", true, "java/lang/Object")
            }

            it("join of class and interface with no inheritance relation should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoClassTypes("C", "IA", false, "java/lang/Object")
            }

        }
    }

    describe("the behavior of the method joinClassTypes(ClassType, Set(ClassType)) of ClassHierarchy") {
        describe("the behavior of joins with classes") {
            describe("the behavior of joins with sets containing one class") {
                it("join class with itself should result in the same class") {
                    testJoinOfClassTypesWithUpperBound("A", "A", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA", "SubA2", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA", "SubA2", false, "A")
                }

                it("join of two indirect subclasses should result in the superclass (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubA", "SubA2", true, "A")
                }

                it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubA", "SubA2", false, "A")
                }

                it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("A", "SubA", true, "A")
                }

                it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("E", "SubE", true, "E")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("E", "C", true, "java/lang/Object")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("E", "C", false, "java/lang/Object")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA2", "SubA3", true, "A")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA2", "SubA3", false, "A")
                }

                it("join of classes with same direct superclass, same interface should result in their superclass and common interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA", "SubA4", true, Set("A", "ID"))
                }

                it("join of classes with same direct superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA", "SubA4", false, Set("A", "ID"))
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubA", "SubSubA2", true, "A")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubA", "SubSubA2", false, "A")
                }

                it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA4", "SubSubA", true, Set("A", "ID"))
                }

                it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA4", "SubSubA", false, Set("A", "ID"))
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA3", "SubB", true, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA3", "SubB", false, "java/lang/Object")
                }

                it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubB", "SubC", true, "SubIB")
                }

                it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubB", "SubC", false, "SubIB")
                }

                it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA", "SubSubA", true, "SubA")
                }

                it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubB", "SubB2", true, Set("B", "SubIB"))
                }

                it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubB", "SubB2", false, Set("B", "SubIB"))
                }
            }
        }

        describe("the behavior of joins with interfaces") {
            describe("the behavior of joins with sets containing one interfaces") {
                it("join interface with itself should result in the same interface") {
                    testJoinOfClassTypesWithUpperBound("SubIB", "SubIB", true, "SubIB")
                }

                it("join of interface and its direct superinterface should result in the interface") {
                    testJoinOfClassTypesWithUpperBound("ID", "SubID", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID", "SubID2", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID", "SubID2", false, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID", "SubID2", true, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID", "SubID2", false, "ID")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("IE", "SubID", true, "java/lang/Object")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("IE", "SubID", false, "java/lang/Object")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID", "SubSubIDSubIA", true, "SubID")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID", "SubSubIDSubIA", false, "SubID")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", true, Set("SubID", "IA"))
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", false, Set("SubID", "IA"))
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID2", "SubSubID", true, "ID")
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID2", "SubSubID", false, "ID")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIDSubIA", "SubSubIDSubIA", true, Set("ID", "IA"))
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIDSubIA", "SubSubIDSubIA", false, Set("ID", "IA"))
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIDSubIA", "SubIB", true, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIDSubIA", "SubIB", false, "java/lang/Object")
                }

                it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID2", "SubID2", true, "SubID2")
                }
            }

            describe("the behavior of joins with sets containing more than one interface") {
                it("join of a interface and several direct subinterfaces should result in the interface") {
                    testJoinOfClassTypesWithUpperBound("ID", Set("SubSubID", "SubSubID2"), true, "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID3", Set("SubID2", "SubID"), true, "ID")
                    testJoinOfClassTypesWithUpperBound("SubID2", Set("SubID", "SubID3"), true, "ID")
                    testJoinOfClassTypesWithUpperBound("SubID", Set("SubID3", "SubID2"), true, "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID3", Set("SubID2", "SubID"), false, "ID")
                    testJoinOfClassTypesWithUpperBound("SubID2", Set("SubID", "SubID3"), false, "ID")
                    testJoinOfClassTypesWithUpperBound("SubID", Set("SubID3", "SubID2"), false, "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID", Set("SubID2", "SubID3"), true, "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubID", Set("SubID2", "SubID3"), false, "ID")
                    testJoinOfClassTypesWithUpperBound("SubID3", Set("SubSubID", "SubID2"), false, "ID")
                    testJoinOfClassTypesWithUpperBound("SubID2", Set("SubSubID", "SubID3"), false, "ID")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIB", Set("ID", "IA", "IC"), true, "java/lang/Object")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIB", Set("ID", "IA", "IC"), false, "java/lang/Object")
                }

                it("join of several interfaces with the same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubIDSubIA", Set("SubID3", "SubID2"), true, "ID")
                }

                it("join of several interfaces with the same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubIDSubIA", Set("SubID3", "SubID2"), false, "ID")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIB", Set("SubID3", "IA"), true, "java/lang/Object")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubIB", Set("SubID3", "IA"), false, "java/lang/Object")
                }

                it("join of interface and several subinterfaces with re-implemented interface should result in the interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubID2", Set("SubSubID2", "SubSubID22"), true, "SubID2")
                }
            }
        }

        describe("the behavior of joins with classes and interfaces") {
            describe("the behavior of joins with sets containing one interfaces or class") {
                it("join of class and its only interface should result in the interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("A", "IA", true, "IA")
                }

                it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubA", "ID", true, "ID")
                }

                it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("D", "IA", true, "IA")
                }

                it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubSubA", "SubID3", true, "SubID3")
                }

                it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubB", "SubIB", true, "SubIB")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("C", "IA", true, "java/lang/Object")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (non-reflexive)") {
                    testJoinOfClassTypesWithUpperBound("C", "IA", false, "java/lang/Object")
                }
            }
            describe("the behavior of joins with sets containing one interfaces and one class") {

                it("join of class and superclass and superinterface should result in both (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("SubB", Set("SubIB", "B"), true, Set("SubIB", "B"))
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinOfClassTypesWithUpperBound("B", Set("SubE", "IA"), true, "java/lang/Object")
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (nonreflexive)") {
                    testJoinOfClassTypesWithUpperBound("B", Set("SubE", "IA"), false, "java/lang/Object")
                }
            }
        }
    }

    describe("the behavior of the method joinClassTypesUntilSingleUpperBound of ClassHierarchy") {
        describe("the behavior of joins with classes") {
            describe("the behavior of joins with sets containing one class") {
                it("join class with itself should result in the same class") {
                    testJoinClassTypesUntilSingleUpperBound("A", "A", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA", "SubA2", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA", "SubA2", false, "A")
                }

                it("join of two indirect subclasses should result in the superclass (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubA", "SubA2", true, "A")
                }

                it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubA", "SubA2", false, "A")
                }

                it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("A", "SubA", true, "A")
                }

                it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("E", "SubE", true, "E")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("E", "C", true, "java/lang/Object")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("E", "C", false, "java/lang/Object")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA2", "SubA3", true, "A")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA2", "SubA3", false, "A")
                }

                it("join of classes with same direct superclass, same interface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA", "SubA4", true, "java/lang/Object")
                }

                it("join of classes with same direct superclass, same interface should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA", "SubA4", false, "java/lang/Object")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubA", "SubSubA2", true, "A")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubA", "SubSubA2", false, "A")
                }

                it("join of classes with same indirect superclass, same interface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA4", "SubSubA", true, "java/lang/Object")
                }

                it("join of classes with same indirect superclass, same interface should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA4", "SubSubA", false, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA3", "SubB", true, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA3", "SubB", false, "java/lang/Object")
                }

                it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubB", "SubC", true, "SubIB")
                }

                it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubB", "SubC", false, "SubIB")
                }

                it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubA", "SubSubA", true, "SubA")
                }

                it("join of two subclasses with a re-implemented interface should result in the re-implemented interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubB", "SubB2", true, "IB")
                }

                it("join of two subclasses with a re-implemented interface should result in the re-implemented interface (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubB", "SubB2", false, "IB")
                }
            }
        }

        describe("the behavior of joins with interfaces") {
            describe("the behavior of joins with sets containing one interfaces") {
                it("join interface with itself should result in the same interface") {
                    testJoinClassTypesUntilSingleUpperBound("SubIB", "SubIB", true, "SubIB")
                }

                it("join of interface and its direct superinterface should result in the interface") {
                    testJoinClassTypesUntilSingleUpperBound("ID", "SubID", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubID", "SubID2", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubID", "SubID2", false, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubID", "SubID2", true, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubID", "SubID2", false, "ID")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("IE", "SubID", true, "java/lang/Object")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("IE", "SubID", false, "java/lang/Object")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubID", "SubSubIDSubIA", true, "SubID")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubID", "SubSubIDSubIA", false, "SubID")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", true, "java/lang/Object")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(
                        "SubSubIDSubIA",
                        "SubSubIDSubIA2",
                        false,
                        "java/lang/Object"
                    )
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubID2", "SubSubID", true, "ID")
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubID2", "SubSubID", false, "ID")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubIDSubIA", "SubSubIDSubIA", true, "java/lang/Object")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubIDSubIA", "SubSubIDSubIA", false, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubIDSubIA", "SubIB", true, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubIDSubIA", "SubIB", false, "java/lang/Object")
                }

                it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound("SubSubID2", "SubID2", true, "SubID2")
                }
            }

            describe("the behavior of joins with sets containing more than one interface") {
                it("join a set of interfaces should result in java/lang/Object") {
                    testJoinClassTypesUntilSingleUpperBound(Set("IA", "IB", "IC"), "java/lang/Object")
                }

                it("join of a interface and several direct subinterfaces should result in the interface") {
                    testJoinClassTypesUntilSingleUpperBound(Set("ID", "SubSubID", "SubSubID2"), "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubID3", "SubID2", "SubID"), "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubSubID", "SubID2", "SubID3"), "ID")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("ID", "IA", "IC", "SubIB"), "java/lang/Object")
                }

                it("join of several interfaces with the same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubID3", "SubID2", "SubSubIDSubIA"), "ID")
                }

                it("join of several interfaces with the same direct superinterface and another common superinterface should result in their common superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(
                        Set("SubID2", "SubSubIDSubIA", "SubID3", "SubSubIDSubIA2"),
                        "ID"
                    )
                }

                it("join of several interfaces with same indirect superinterface and a different superinterface should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(
                        Set("SubSubID2", "SubSubIDSubIA", "SubID3", "SubIB"),
                        "java/lang/Object"
                    )
                }

                it("join of several interfaces with same indirect superinterface and another common superinterface should result in indirect superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(
                        Set("SubSubID2", "SubSubIDSubIA", "SubID3", "SubIDSubIA"),
                        "ID"
                    )
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubIB", "SubID3", "IA"), "java/lang/Object")
                }

                it("join of interface and several subinterfaces with re-implemented interface should result in the interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubSubID2", "SubSubID22", "SubID2"), "SubID2")
                }
            }
        }

        describe("the behavior of joins with classes and interfaces") {
            describe("the behavior of joins with sets containing one interfaces or class") {
                it("join of class and its only interface should result in the interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("A", "IA"), "IA")
                }

                it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubA", "ID"), "ID")
                }

                it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("D", "IA"), "IA")
                }

                it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubSubA", "SubID3"), "SubID3")
                }

                it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubB", "SubIB"), "SubIB")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("C", "IA"), "java/lang/Object")
                }

            }
            describe("the behavior of joins with sets containing one interfaces and one class") {

                it("join of class and superclass and interface should result in their superinterface (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("SubB", "SubIB", "B"), "IB")
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinClassTypesUntilSingleUpperBound(Set("B", "SubE", "IA"), "java/lang/Object")
                }

            }
        }
    }
}
