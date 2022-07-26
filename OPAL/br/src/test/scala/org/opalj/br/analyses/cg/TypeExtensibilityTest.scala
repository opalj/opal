/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import com.typesafe.config.ConfigFactory
import org.opalj.br.TestSupport.biProject
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
 * This tests the basic functionality of the [[TypeExtensibilityKey]] and determines whether the
 * computed information is correct. Beneath the basics, the test also contains an integration test
 * that ensures that all directly extensible types (see [[ClassExtensibilityTest]]) are also
 * transitively extensible.
 *
 * @author Michael Reif
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class TypeExtensibilityTest extends AnyFunSpec with Matchers {

    val testProject = biProject("extensible_classes.jar")

    /*
     * Can be used as prefix when ObjectTypes are created.
     */
    val testPackage = "extensible_classes/transitivity/"

    def mergeConfigString(extConf: String, pkgConf: String): String = extConf+"\n"+pkgConf

    describe("all directly extensible types") {

        val openConf = ConfigFactory.parseString(
            mergeConfigString(
                ClassExtensibilityConfig.classExtensibilityAnalysis,
                ClosedPackagesConfig.openCodeBase
            )
        )

        val closedConf = ConfigFactory.parseString(
            mergeConfigString(
                ClassExtensibilityConfig.classExtensibilityAnalysis,
                ClosedPackagesConfig.allPackagesClosed
            )
        )

        it("should also be transitively extensible") {

            // open package
            var project = Project.recreate(testProject, openConf, true)
            var isDirectlyExtensible = project.get(ClassExtensibilityKey)
            var isExtensible = project.get(TypeExtensibilityKey)

            var relevantTypes = for {
                cf <- project.allClassFiles if (isDirectlyExtensible(cf.thisType).isYes)
            } yield cf.thisType

            if (relevantTypes.isEmpty)
                fail("No directly extensible types found!")

            relevantTypes.foreach { objectType =>
                isExtensible(objectType) should be(Yes)
            }

            // closed package
            project = Project.recreate(project, closedConf, true)
            isDirectlyExtensible = project.get(ClassExtensibilityKey)
            isExtensible = project.get(TypeExtensibilityKey)

            relevantTypes = for {
                cf <- project.allClassFiles if (isDirectlyExtensible(cf.thisType).isYes)
            } yield cf.thisType

            if (relevantTypes.isEmpty)
                fail("No directly extensible types found!")

            relevantTypes.foreach { objectType => isExtensible(objectType) should be(Yes) }
        }
    }

    describe("when a type is located in a closed package") {

        val configString = mergeConfigString(
            ClassExtensibilityConfig.classExtensibilityAnalysis,
            ClosedPackagesConfig.allPackagesClosed
        )

        val config = ConfigFactory.parseString(configString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(TypeExtensibilityKey)

        it("a package visible class is transitively extensible when it has a public subclass") {
            val objectType = ObjectType(s"${testPackage}case1/Class")
            isExtensible(objectType) should be(Yes)
        }

        it("a package visible class should NOT be transitively extensible when all subclasses"+
            " are (effectively) final") {
            val classOt = ObjectType(s"${testPackage}case2/Class")
            val interfaceOt = ObjectType(s"${testPackage}case2/Interface")

            isExtensible(classOt) should be(No)
            isExtensible(interfaceOt) should be(No)
        }

        it("a non-final public class is transitively extensible even when all subclasses are NOT") {
            val pClassOt = ObjectType(s"${testPackage}case3/PublicClass")

            val pfClassOt = ObjectType(s"${testPackage}case3/PublicFinalClass")
            val classOt = ObjectType(s"${testPackage}case3/Class")
            val pEfClassOt = ObjectType(s"${testPackage}case3/EffectivelyFinalClass")

            isExtensible(pClassOt) should be(Yes)

            isExtensible(pfClassOt) should be(No)
            isExtensible(classOt) should be(No)
            isExtensible(pEfClassOt) should be(No)
        }

        it("a non-final package visible class must be transitively extensible even if only one subtype"+
            "is extensible") {

            val rootOt = ObjectType(s"${testPackage}case5/TransitivelyExtensible")

            val pClassOt = ObjectType(s"${testPackage}case5/PublicClass")
            val pfClassOt = ObjectType(s"${testPackage}case5/PublicFinalClass")

            isExtensible(pClassOt) should be(Yes)
            isExtensible(pfClassOt) should be(No)

            isExtensible(rootOt) should be(Yes)
        }

        it("a non-final package visible class must be transitively extensible even if only one subtype"+
            "is extensible and the type hierarchy is large") {

            val rootOt = ObjectType(s"${testPackage}case6/TransitivelyExtensible")

            val directSt1 = ObjectType(s"${testPackage}case6/HiddenSubclass")
            val directSt2 = ObjectType(s"${testPackage}case6/PublicFinalClass")

            isExtensible(directSt1) should be(Yes)
            isExtensible(directSt2) should be(No)

            isExtensible(rootOt) should be(Yes)
        }
    }

    describe("when a type belongs to an open package") {

        val configString = mergeConfigString(
            ClassExtensibilityConfig.classExtensibilityAnalysis,
            ClosedPackagesConfig.openCodeBase
        )

        val config = ConfigFactory.parseString(configString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(TypeExtensibilityKey)
        val isClassExtensible = project.get(ClassExtensibilityKey)

        it("a package visible class should be transitively extensible") {
            val case1_classOt = ObjectType(s"${testPackage}case1/Class")
            val case2_classOt = ObjectType(s"${testPackage}case2/Class")
            val case2_interfaceOt = ObjectType(s"${testPackage}case2/Interface")

            isExtensible(case1_classOt) should be(Yes)
            isExtensible(case2_classOt) should be(Yes)
            isExtensible(case2_interfaceOt) should be(Yes)
        }

        it("a type from which an application type inherits from is (obviously) extensible") {
            val hashSetObjectType = ObjectType("java/util/HashSet")
            assert(project.classFile(hashSetObjectType).isEmpty)
            assert(isClassExtensible(hashSetObjectType) == Unknown)

            isExtensible(hashSetObjectType) should be(Yes)
        }

        it("the extensibility of an unknown type should be unknown") {
            val unknownType = ObjectType("unknown/Unknown")
            assert(project.classFile(unknownType).isEmpty)
            assert(isClassExtensible(unknownType) == Unknown)

            isExtensible(unknownType) should be(Unknown)
        }
    }
}
