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
 * @author Michael Reif
 */
class ClassExtensibilityTest extends AnyFunSpec with Matchers {

    val testProject = biProject("extensible_classes.jar")

    /*
     * Can be used as prefix when ObjectTypes are created.
     */
    val testPackage = "extensible_classes/visibility/"

    private def TypeFixture(simpleName: String) = ObjectType(testPackage + simpleName)

    val PublicInterface = TypeFixture("PublicInterface")
    val Interface = TypeFixture("Interface")
    val Annotation = TypeFixture("Annotation")
    val PublicAnnotation = TypeFixture("PublicAnnotation")
    val FinalClass = TypeFixture("FinalClass")
    val Class = TypeFixture("Class")
    val PublicFinalClass = TypeFixture("PublicFinalClass")
    val PublicClass = TypeFixture("PublicClass")
    val PublicClassWithPrivateConstructor = TypeFixture("PublicClassWithPrivateConstructor")
    val PublicEnum = TypeFixture("PublicEnum")
    val Enum = TypeFixture("Enum")

    def mergeConfigString(extConf: String, pkgConf: String): String = extConf+"\n"+pkgConf

    describe("when a type is located in an open package") {

        val configString = mergeConfigString(
            ClassExtensibilityConfig.classExtensibilityAnalysis,
            ClosedPackagesConfig.openCodeBase
        )

        val config = ConfigFactory.parseString(configString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(ClassExtensibilityKey)

        it("non-final types should be extensible") {
            isExtensible(PublicClass) should be(Yes)
            isExtensible(PublicInterface) should be(Yes)
        }

        it("package visible non-final types should be extensible") {
            isExtensible(Class) should be(Yes)
            isExtensible(Interface) should be(Yes)
        }

        it("effectively final types should not be extensible") {
            isExtensible(PublicClassWithPrivateConstructor) should be(No)
        }

        it("final types should be not extensible") {
            isExtensible(PublicFinalClass) should be(No)
            isExtensible(Enum) should be(No)
            isExtensible(PublicEnum) should be(No)
            isExtensible(Annotation) should be(No)
            isExtensible(PublicAnnotation) should be(No)
        }
    }

    describe("when a type is located in a closed package") {

        val configString = mergeConfigString(
            ClassExtensibilityConfig.classExtensibilityAnalysis,
            ClosedPackagesConfig.allPackagesClosed
        )

        val config = ConfigFactory.parseString(configString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(ClassExtensibilityKey)

        it("public non-final types should be extensible") {
            isExtensible(PublicClass) should be(Yes)
            isExtensible(PublicInterface) should be(Yes)
        }

        it("package visible non-final types should NOT be directly extensible") {
            isExtensible(Class) should be(No)
            isExtensible(Interface) should be(No)
        }

        it("effectively final types should not be extensible") {
            isExtensible(PublicClassWithPrivateConstructor) should be(No)
        }

        it("final types should be not extensible") {
            isExtensible(PublicFinalClass) should be(No)
            isExtensible(Enum) should be(No)
            isExtensible(PublicEnum) should be(No)
            isExtensible(Annotation) should be(No)
            isExtensible(PublicAnnotation) should be(No)
        }
    }

    describe("when a type is configured as extensible") {

        val forcedExtensibleClasses = List(PublicFinalClass, PublicClassWithPrivateConstructor).
            map(_.fqn.replaceAll("[.]", "/"))

        val confString = mergeConfigString(
            ClassExtensibilityConfig.configuredExtensibleClasses(forcedExtensibleClasses),
            ClosedPackagesConfig.openCodeBase
        )

        val config = ConfigFactory.parseString(confString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(ClassExtensibilityKey)

        it("it should be extensible, no matter what") {
            isExtensible(PublicFinalClass) should be(Yes)
            isExtensible(PublicClassWithPrivateConstructor) should be(Yes)
        }
    }

    describe("when a type is configured as final") {

        val forcedExtensibleClasses = List(PublicClass, PublicInterface).
            map(_.fqn.replaceAll("[.]", "/"))

        val confString = mergeConfigString(
            ClassExtensibilityConfig.configuredFinalClasses(forcedExtensibleClasses),
            ClosedPackagesConfig.allPackagesClosed
        )

        val config = ConfigFactory.parseString(confString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(ClassExtensibilityKey)

        it("it should NOT be extensible, no matter what") {
            isExtensible(PublicClass) should be(No)
            isExtensible(PublicInterface) should be(No)
        }
    }
}

object ClassExtensibilityConfig {

    val classExtensibilityAnalysis =
        """
          |org.opalj.br.analyses.cg.ClassExtensibilityKey {
          |    analysis = "org.opalj.br.analyses.cg.DefaultClassExtensibility"
          |}
        """.stripMargin

    def configuredExtensibleClasses(types: List[String] = List.empty) =
        s"""
           |org.opalj.br.analyses.cg.ClassExtensibilityKey {
           |    analysis = "org.opalj.br.analyses.cg.ConfiguredExtensibleClasses"
           |    extensibleClasses = [${types.map("\""+_+"\"").mkString(",")}]
           |}
          """.stripMargin

    def configuredFinalClasses(types: List[String] = List.empty) =
        s"""
           |org.opalj.br.analyses.cg.ClassExtensibilityKey {
           |    analysis = "org.opalj.br.analyses.cg.ConfiguredFinalClasses"
           |    finalClasses = [${types.map("\""+_+"\"").mkString(",")}]
           |}
        """.stripMargin

}

object ClosedPackagesConfig {

    val openCodeBase =
        """
          |org.opalj.br.analyses.cg.ClosedPackagesKey {
          |    analysis = "org.opalj.br.analyses.cg.OpenCodeBase"
          |}
        """.stripMargin

    val allPackagesClosed =
        """
          |org.opalj.br.analyses.cg.ClosedPackagesKey {
          |    analysis = "org.opalj.br.analyses.cg.AllPackagesClosed"
          |}
        """.stripMargin

    def configureClosedPackages(regex: String = "java(/.*)") =
        s"""
           |org.opalj.br.analyses.cg.ClosedPackagesKey {
           |    analysis = "org.opalj.br.analyses.cg.ClosedPackagesConfiguration"
           |    closedPackages = "$regex"
           |}
        """.stripMargin

    def configureOpenPackages(regex: String = "^.*$") =
        s"""
           |org.opalj.br.analyses.cg.ClosedPackagesKey {
           |    analysis = "org.opalj.br.analyses.cg.ClosedPackagesConfiguration"
           |    closedPackages = "$regex"
           |}
        """.stripMargin
}
