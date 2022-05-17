/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.collection.immutable.UIDSet
import org.opalj.log.GlobalLogContext

/**
 * @author Tobias Becker
 */
@RunWith(classOf[JUnitRunner])
class JoinObjectTypesTest extends AnyFunSpec with Matchers {

    final val classhierachy = {
        //val thisClass = classOf[org.opalj.br.analyses.JoinObjectTypesTest]
        val thisClass = this.getClass()
        val in = thisClass.getResourceAsStream("ClassHierarchyUpperBounds.ths")
        if (in == null)
            throw new UnknownError("class hierarchy could not be loaded")
        ClassHierarchy(
            Iterable.empty,
            List(() => in)
        )(GlobalLogContext)
    }

    implicit def stringToUIDSetObjectType(str: String) = UIDSet(ObjectType(str))
    implicit def stringToObjectType(str: String) = ObjectType(str)
    implicit def setToUIDSet(s: Set[String]): UIDSet[ObjectType] = {
        UIDSet.empty[ObjectType] ++ s.map(ObjectType.apply)
    }

    def mkString(param: UIDSet[ObjectType]) = {
        param.toSeq.map(_.toJava).mkString("{", ",", "}")
    }

    def testJoinOfTwoObjectTypes(
        param1:    ObjectType,
        param2:    ObjectType,
        reflexive: Boolean,
        expected:  UIDSet[ObjectType]
    ) = {
        val result = classhierachy.joinObjectTypes(param1, param2, reflexive)
        if (result != expected)
            fail(
                s"${param1} join${if (reflexive) "(reflexive)" else ""}"+
                    s" ${param2} is ${mkString(result)};"+
                    s" expected ${mkString(expected)}"
            )
    }

    def testJoinOfObjectTypesWithUpperBound(
        param1:    ObjectType,
        param2:    UIDSet[ObjectType],
        reflexive: Boolean,
        expected:  UIDSet[ObjectType]
    ) = {
        val result = classhierachy.joinObjectTypes(param1, param2, reflexive)
        if (result != expected)
            fail(
                s"$param1 join${if (reflexive) "(reflexive)" else ""}"+
                    s" ${mkString(param2)} is ${mkString(result)};"+
                    s" expected ${mkString(expected)}"
            )
    }

    def testJoinObjectTypesUntilSingleUpperBound(
        param1:    ObjectType,
        param2:    ObjectType,
        reflexive: Boolean,
        expected:  ObjectType
    ) = {
        val result = classhierachy.joinObjectTypesUntilSingleUpperBound(param1, param2, reflexive)
        if (result != expected)
            fail(s"$param1 join $param2 ${if (reflexive) "(reflexive)" else ""}"+
                s" with joinObjectTypesUntilSingleUpperBound(ObjectType, ObjectType, Boolean) is ${result};"+
                s" expected ${expected}")
    }

    def testJoinObjectTypesUntilSingleUpperBound(
        param:    UIDSet[ObjectType],
        expected: ObjectType
    ) = {
        val result = classhierachy.joinObjectTypesUntilSingleUpperBound(param)
        if (result != expected)
            fail(s"join of ${mkString(param)}"+
                s" using joinObjectTypesUntilSingleUpperBound(UIDSet[ObjectType]) is ${result};"+
                s" expected ${expected}")

    }

    describe("the behavior of the method joinObjectTypes(ObjectType,ObjectType) of ClassHierachy") {
        // uncomment to display the test graph:
        // io.writeAndOpen((toDot.generateDot(Set(classhierachy.toGraph))), "test", ".dot")

        describe("the behavior of joins with classes") {

            it("join class with itself should result in the same class") {
                testJoinOfTwoObjectTypes("A", "A", true, "A")
            }

            it("join of two direct subclasses should result in the superclass (reflexive)") {
                testJoinOfTwoObjectTypes("SubA", "SubA2", true, "A")
            }

            it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubA", "SubA2", false, "A")
            }

            it("join of two indirect subclasses should result in the superclass (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubA", "SubA2", true, "A")
            }

            it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubSubA", "SubA2", false, "A")
            }

            it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                testJoinOfTwoObjectTypes("A", "SubA", true, "A")
            }

            it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                testJoinOfTwoObjectTypes("E", "SubE", true, "E")
            }

            it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                testJoinOfTwoObjectTypes("E", "C", true, "java/lang/Object")
            }

            it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoObjectTypes("E", "C", false, "java/lang/Object")
            }

            it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                testJoinOfTwoObjectTypes("SubA2", "SubA3", true, "A")
            }

            it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubA2", "SubA3", false, "A")
            }

            it("join of classes with same direct superclass, same interface should result in their superclass and common interface (reflexive)") {
                testJoinOfTwoObjectTypes("SubA", "SubA4", true, Set("A", "ID"))
            }

            it("join of classes with same direct superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubA", "SubA4", false, Set("A", "ID"))
            }

            it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubA", "SubSubA2", true, "A")
            }

            it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubSubA", "SubSubA2", false, "A")
            }

            it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (reflexive)") {
                testJoinOfTwoObjectTypes("SubA4", "SubSubA", true, Set("A", "ID"))
            }

            it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubA4", "SubSubA", false, Set("A", "ID"))
            }

            it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                testJoinOfTwoObjectTypes("SubA3", "SubB", true, "java/lang/Object")
            }

            it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubA3", "SubB", false, "java/lang/Object")
            }

            it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                testJoinOfTwoObjectTypes("SubB", "SubC", true, "SubIB")
            }

            it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubB", "SubC", false, "SubIB")
            }

            it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                testJoinOfTwoObjectTypes("SubA", "SubSubA", true, "SubA")
            }

            it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubB", "SubB2", true, Set("B", "SubIB"))
            }

            it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubB", "SubB2", false, Set("B", "SubIB"))
            }

        }

        describe("the behavior of joins with interfaces") {

            it("join interface with itself should result in the same interface") {
                testJoinOfTwoObjectTypes("SubIB", "SubIB", true, "SubIB")
            }

            it("join of interface and its direct superinterface should result in the interface") {
                testJoinOfTwoObjectTypes("ID", "SubID", true, "ID")
            }

            it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubID", "SubID2", true, "ID")
            }

            it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubID", "SubID2", false, "ID")
            }

            it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubID", "SubID2", true, "ID")
            }

            it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubSubID", "SubID2", false, "ID")
            }

            it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                testJoinOfTwoObjectTypes("IE", "SubID", true, "java/lang/Object")
            }

            it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoObjectTypes("IE", "SubID", false, "java/lang/Object")
            }

            it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubID", "SubSubIDSubIA", true, "SubID")
            }

            it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubSubID", "SubSubIDSubIA", false, "SubID")
            }

            it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubIDSubIA", "SubSubIDSubIA2", true, Set("SubID", "IA"))
            }

            it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubSubIDSubIA", "SubSubIDSubIA2", false, Set("SubID", "IA"))
            }

            it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubID2", "SubSubID", true, "ID")
            }

            it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubID2", "SubSubID", false, "ID")
            }

            it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                testJoinOfTwoObjectTypes("SubIDSubIA", "SubSubIDSubIA", true, Set("ID", "IA"))
            }

            it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubIDSubIA", "SubSubIDSubIA", false, Set("ID", "IA"))
            }

            it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                testJoinOfTwoObjectTypes("SubIDSubIA", "SubIB", true, "java/lang/Object")
            }

            it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoObjectTypes("SubIDSubIA", "SubIB", false, "java/lang/Object")
            }

            it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubID2", "SubID2", true, "SubID2")
            }

        }

        describe("the behavior of joins with classes and interfaces") {
            it("join of class and its only interface should result in the interface (reflexive)") {
                testJoinOfTwoObjectTypes("A", "IA", true, "IA")
            }

            it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubA", "ID", true, "ID")
            }

            it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                testJoinOfTwoObjectTypes("D", "IA", true, "IA")
            }

            it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                testJoinOfTwoObjectTypes("SubSubA", "SubID3", true, "SubID3")
            }

            it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                testJoinOfTwoObjectTypes("SubB", "SubIB", true, "SubIB")
            }

            it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                testJoinOfTwoObjectTypes("C", "IA", true, "java/lang/Object")
            }

            it("join of class and interface with no inheritance relation should result in java/lang/Object (non-reflexive)") {
                testJoinOfTwoObjectTypes("C", "IA", false, "java/lang/Object")
            }

        }
    }

    describe("the behavior of the method joinObjectTypes(ObjectType, Set(ObjectType)) of ClassHierarchy") {
        describe("the behavior of joins with classes") {
            describe("the behavior of joins with sets containing one class") {
                it("join class with itself should result in the same class") {
                    testJoinOfObjectTypesWithUpperBound("A", "A", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA", "SubA2", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA", "SubA2", false, "A")
                }

                it("join of two indirect subclasses should result in the superclass (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubA", "SubA2", true, "A")
                }

                it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubA", "SubA2", false, "A")
                }

                it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("A", "SubA", true, "A")
                }

                it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("E", "SubE", true, "E")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("E", "C", true, "java/lang/Object")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("E", "C", false, "java/lang/Object")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA2", "SubA3", true, "A")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA2", "SubA3", false, "A")
                }

                it("join of classes with same direct superclass, same interface should result in their superclass and common interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA", "SubA4", true, Set("A", "ID"))
                }

                it("join of classes with same direct superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA", "SubA4", false, Set("A", "ID"))
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubA", "SubSubA2", true, "A")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubA", "SubSubA2", false, "A")
                }

                it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA4", "SubSubA", true, Set("A", "ID"))
                }

                it("join of classes with same indirect superclass, same interface should result in their superclass and common interface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA4", "SubSubA", false, Set("A", "ID"))
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA3", "SubB", true, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA3", "SubB", false, "java/lang/Object")
                }

                it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubB", "SubC", true, "SubIB")
                }

                it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubB", "SubC", false, "SubIB")
                }

                it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA", "SubSubA", true, "SubA")
                }

                it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubB", "SubB2", true, Set("B", "SubIB"))
                }

                it("join of two subclasses with a re-implemented interface should result in the superclass and the subinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubB", "SubB2", false, Set("B", "SubIB"))
                }
            }
        }

        describe("the behavior of joins with interfaces") {
            describe("the behavior of joins with sets containing one interfaces") {
                it("join interface with itself should result in the same interface") {
                    testJoinOfObjectTypesWithUpperBound("SubIB", "SubIB", true, "SubIB")
                }

                it("join of interface and its direct superinterface should result in the interface") {
                    testJoinOfObjectTypesWithUpperBound("ID", "SubID", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID", "SubID2", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID", "SubID2", false, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID", "SubID2", true, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID", "SubID2", false, "ID")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("IE", "SubID", true, "java/lang/Object")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("IE", "SubID", false, "java/lang/Object")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID", "SubSubIDSubIA", true, "SubID")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID", "SubSubIDSubIA", false, "SubID")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", true, Set("SubID", "IA"))
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", false, Set("SubID", "IA"))
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID2", "SubSubID", true, "ID")
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID2", "SubSubID", false, "ID")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIDSubIA", "SubSubIDSubIA", true, Set("ID", "IA"))
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in both superinterfaces (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIDSubIA", "SubSubIDSubIA", false, Set("ID", "IA"))
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIDSubIA", "SubIB", true, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIDSubIA", "SubIB", false, "java/lang/Object")
                }

                it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID2", "SubID2", true, "SubID2")
                }
            }

            describe("the behavior of joins with sets containing more than one interface") {
                it("join of a interface and several direct subinterfaces should result in the interface") {
                    testJoinOfObjectTypesWithUpperBound("ID", Set("SubSubID", "SubSubID2"), true, "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID3", Set("SubID2", "SubID"), true, "ID")
                    testJoinOfObjectTypesWithUpperBound("SubID2", Set("SubID", "SubID3"), true, "ID")
                    testJoinOfObjectTypesWithUpperBound("SubID", Set("SubID3", "SubID2"), true, "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID3", Set("SubID2", "SubID"), false, "ID")
                    testJoinOfObjectTypesWithUpperBound("SubID2", Set("SubID", "SubID3"), false, "ID")
                    testJoinOfObjectTypesWithUpperBound("SubID", Set("SubID3", "SubID2"), false, "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID", Set("SubID2", "SubID3"), true, "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubID", Set("SubID2", "SubID3"), false, "ID")
                    testJoinOfObjectTypesWithUpperBound("SubID3", Set("SubSubID", "SubID2"), false, "ID")
                    testJoinOfObjectTypesWithUpperBound("SubID2", Set("SubSubID", "SubID3"), false, "ID")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIB", Set("ID", "IA", "IC"), true, "java/lang/Object")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIB", Set("ID", "IA", "IC"), false, "java/lang/Object")
                }

                it("join of several interfaces with the same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubIDSubIA", Set("SubID3", "SubID2"), true, "ID")
                }

                it("join of several interfaces with the same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubIDSubIA", Set("SubID3", "SubID2"), false, "ID")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIB", Set("SubID3", "IA"), true, "java/lang/Object")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubIB", Set("SubID3", "IA"), false, "java/lang/Object")
                }

                it("join of interface and several subinterfaces with re-implemented interface should result in the interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubID2", Set("SubSubID2", "SubSubID22"), true, "SubID2")
                }
            }
        }

        describe("the behavior of joins with classes and interfaces") {
            describe("the behavior of joins with sets containing one interfaces or class") {
                it("join of class and its only interface should result in the interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("A", "IA", true, "IA")
                }

                it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubA", "ID", true, "ID")
                }

                it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("D", "IA", true, "IA")
                }

                it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubSubA", "SubID3", true, "SubID3")
                }

                it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubB", "SubIB", true, "SubIB")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("C", "IA", true, "java/lang/Object")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (non-reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("C", "IA", false, "java/lang/Object")
                }
            }
            describe("the behavior of joins with sets containing one interfaces and one class") {

                it("join of class and superclass and superinterface should result in both (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("SubB", Set("SubIB", "B"), true, Set("SubIB", "B"))
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinOfObjectTypesWithUpperBound("B", Set("SubE", "IA"), true, "java/lang/Object")
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (nonreflexive)") {
                    testJoinOfObjectTypesWithUpperBound("B", Set("SubE", "IA"), false, "java/lang/Object")
                }
            }
        }
    }

    describe("the behavior of the method joinObjectTypesUntilSingleUpperBound of ClassHierachy") {
        describe("the behavior of joins with classes") {
            describe("the behavior of joins with sets containing one class") {
                it("join class with itself should result in the same class") {
                    testJoinObjectTypesUntilSingleUpperBound("A", "A", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA", "SubA2", true, "A")
                }

                it("join of two direct subclasses should result in the superclass (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA", "SubA2", false, "A")
                }

                it("join of two indirect subclasses should result in the superclass (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubA", "SubA2", true, "A")
                }

                it("join of two indirect subclasses should result in the superclass (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubA", "SubA2", false, "A")
                }

                it("join of a class and a subclass with common implemented interfaces should result in the superclass (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("A", "SubA", true, "A")
                }

                it("join of a class and a subclass with no common implemented interfaces should result in the class (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("E", "SubE", true, "E")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("E", "C", true, "java/lang/Object")
                }

                it("join of class with no interface and no superclass and another class should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("E", "C", false, "java/lang/Object")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA2", "SubA3", true, "A")
                }

                it("join of classes with same direct superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA2", "SubA3", false, "A")
                }

                it("join of classes with same direct superclass, same interface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA", "SubA4", true, "java/lang/Object")
                }

                it("join of classes with same direct superclass, same interface should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA", "SubA4", false, "java/lang/Object")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubA", "SubSubA2", true, "A")
                }

                it("join of classes with same indirect superclass, different interface should result in their superclass (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubA", "SubSubA2", false, "A")
                }

                it("join of classes with same indirect superclass, same interface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA4", "SubSubA", true, "java/lang/Object")
                }

                it("join of classes with same indirect superclass, same interface should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA4", "SubSubA", false, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA3", "SubB", true, "java/lang/Object")
                }

                it("join of classes with different superclass, different interface should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA3", "SubB", false, "java/lang/Object")
                }

                it("join of classes with different superclass, same interface should result in their common interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubB", "SubC", true, "SubIB")
                }

                it("join of classes with different superclass, same interface should result in their common interface (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubB", "SubC", false, "SubIB")
                }

                it("join of class and subclass with re-implemented interface should result in the class (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubA", "SubSubA", true, "SubA")
                }

                it("join of two subclasses with a re-implemented interface should result in the re-implemented interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubB", "SubB2", true, "IB")
                }

                it("join of two subclasses with a re-implemented interface should result in the re-implemented interface (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubB", "SubB2", false, "IB")
                }
            }
        }

        describe("the behavior of joins with interfaces") {
            describe("the behavior of joins with sets containing one interfaces") {
                it("join interface with itself should result in the same interface") {
                    testJoinObjectTypesUntilSingleUpperBound("SubIB", "SubIB", true, "SubIB")
                }

                it("join of interface and its direct superinterface should result in the interface") {
                    testJoinObjectTypesUntilSingleUpperBound("ID", "SubID", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubID", "SubID2", true, "ID")
                }

                it("join of two direct subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubID", "SubID2", false, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubID", "SubID2", true, "ID")
                }

                it("join of two indirect subinterfaces should result in the superinterface (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubID", "SubID2", false, "ID")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("IE", "SubID", true, "java/lang/Object")
                }

                it("join of interface with no superinterface and another interface should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("IE", "SubID", false, "java/lang/Object")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubID", "SubSubIDSubIA", true, "SubID")
                }

                it("join of interfaces with same direct superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubID", "SubSubIDSubIA", false, "SubID")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", true, "java/lang/Object")
                }

                it("join of interfaces with same direct superinterface and another common superinterface should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubIDSubIA", "SubSubIDSubIA2", false, "java/lang/Object")
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubID2", "SubSubID", true, "ID")
                }

                it("join of interfaces with same indirect superinterface and a different superinterface should result in their common superinterface (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubID2", "SubSubID", false, "ID")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubIDSubIA", "SubSubIDSubIA", true, "java/lang/Object")
                }

                it("join of interfaces with same indirect superinterface and another common superinterface should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubIDSubIA", "SubSubIDSubIA", false, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubIDSubIA", "SubIB", true, "java/lang/Object")
                }

                it("join of interfaces with different superinterfaces should result in java/lang/Object (non-reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubIDSubIA", "SubIB", false, "java/lang/Object")
                }

                it("join of interface and subinterface with re-implemented interface should result in the interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound("SubSubID2", "SubID2", true, "SubID2")
                }
            }

            describe("the behavior of joins with sets containing more than one interface") {
                it("join a set of interfaces should result in java/lang/Object") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("IA", "IB", "IC"), "java/lang/Object")
                }

                it("join of a interface and several direct subinterfaces should result in the interface") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("ID", "SubSubID", "SubSubID2"), "ID")
                }

                it("join of several direct subinterfaces should result in the superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubID3", "SubID2", "SubID"), "ID")
                }

                it("join of several indirect subinterfaces should result in the superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubSubID", "SubID2", "SubID3"), "ID")
                }

                it("join of several interfaces with no superinterface and another interface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("ID", "IA", "IC", "SubIB"), "java/lang/Object")
                }

                it("join of several interfaces with the same direct superinterface and a different superinterface should result in their common superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubID3", "SubID2", "SubSubIDSubIA"), "ID")
                }

                it("join of several interfaces with the same direct superinterface and another common superinterface should result in their common superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubID2", "SubSubIDSubIA", "SubID3", "SubSubIDSubIA2"), "ID")
                }

                it("join of several interfaces with same indirect superinterface and a different superinterface should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubSubID2", "SubSubIDSubIA", "SubID3", "SubIB"), "java/lang/Object")
                }

                it("join of several interfaces with same indirect superinterface and another common superinterface should result in indirect superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubSubID2", "SubSubIDSubIA", "SubID3", "SubIDSubIA"), "ID")
                }

                it("join of several interfaces with different superinterfaces should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubIB", "SubID3", "IA"), "java/lang/Object")
                }

                it("join of interface and several subinterfaces with re-implemented interface should result in the interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubSubID2", "SubSubID22", "SubID2"), "SubID2")
                }
            }
        }

        describe("the behavior of joins with classes and interfaces") {
            describe("the behavior of joins with sets containing one interfaces or class") {
                it("join of class and its only interface should result in the interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("A", "IA"), "IA")
                }

                it("join of class and a superinterface of its only interface should result in the superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubA", "ID"), "ID")
                }

                it("join of class with several interfaces and one of those interfaces should result in this interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("D", "IA"), "IA")
                }

                it("join of class with several interfaces and one of those interfaces superinterface should result in this superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubSubA", "SubID3"), "SubID3")
                }

                it("join of class and a re-implemented interface should result in the interface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubB", "SubIB"), "SubIB")
                }

                it("join of class and interface with no inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("C", "IA"), "java/lang/Object")
                }

            }
            describe("the behavior of joins with sets containing one interfaces and one class") {

                it("join of class and superclass and interface should result in their superinterface (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("SubB", "SubIB", "B"), "IB")
                }

                it("join of interface and a class/interface-pair where no two are in an inheritance relation should result in java/lang/Object (reflexive)") {
                    testJoinObjectTypesUntilSingleUpperBound(Set("B", "SubE", "IA"), "java/lang/Object")
                }

            }
        }
    }
}
