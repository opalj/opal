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
package br
package analyses

import org.scalatest.Matchers
import org.scalatest.FunSpec
import com.typesafe.config.ConfigFactory
import org.opalj.bi.TestSupport.locateTestResources

/**
 *
 *
 * @author Michael Reif
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DirectTypeExtensibilityTest extends FunSpec with Matchers {

    val testProject = Project(locateTestResources("extensible_classes.jar", "bi"))

    /*
    * Can be used as prefix when ObjectTypes are created.
     */
    val testPackage = "extensible_classes/visibility/"

    val PublicInterface = ObjectType(testPackage+"PublicInterface")
    val Interface = ObjectType(testPackage+"Interface")
    val Annotation = ObjectType(testPackage+"Annotation")
    val PublicAnnotation = ObjectType(testPackage+"PublicAnnotation")
    val FinalClass = ObjectType(testPackage+"FinalClass")
    val Class = ObjectType(testPackage+"Class")
    val PublicFinalClass = ObjectType(testPackage+"PublicFinalClass")
    val PublicClass = ObjectType(testPackage+"PublicClass")
    val PublicClassWithPrivConstructors = ObjectType(testPackage+"PublicClassWithPrivConstructors")
    val PublicEnum = ObjectType(testPackage+"PublicEnum")
    val Enum = ObjectType(testPackage+"Enum")

    def mergeConfigString(extConf: String, pkgConf: String): String = {
        extConf+"\n"+pkgConf
    }

    describe("when a type is located in an open package") {

        val confString = mergeConfigString(
            DirectTypeExtensibilityConfig.directTypeExtensibilityAnalysis,
            ClosedPackagesConfig.openCodeBase
        )

        val config = ConfigFactory.parseString(confString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(DirectTypeExtensibilityKey)

        it("non-final types should be extensible") {
            isExtensible(PublicClass) should be(Yes)
            isExtensible(PublicInterface) should be(Yes)
        }

        it("package visible non-final types should be extensible") {
            isExtensible(Class) should be(Yes)
            isExtensible(Interface) should be(Yes)
        }

        it("effectively final types should not be extensible") {
            isExtensible(PublicClassWithPrivConstructors) should be(No)
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

        val confString = mergeConfigString(
            DirectTypeExtensibilityConfig.directTypeExtensibilityAnalysis,
            ClosedPackagesConfig.closedCodeBase
        )

        val config = ConfigFactory.parseString(confString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(DirectTypeExtensibilityKey)

        it("public non-final types should be extensible") {
            isExtensible(PublicClass) should be(Yes)
            isExtensible(PublicInterface) should be(Yes)
        }

        it("package visible non-final types should NOT be directly extensible") {
            isExtensible(Class) should be(No)
            isExtensible(Interface) should be(No)
        }

        it("effectively final types should not be extensible") {
            isExtensible(PublicClassWithPrivConstructors) should be(No)
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

        val forcedExtensibleTyptes = List(PublicFinalClass, PublicClassWithPrivConstructors).
            map(_.fqn.replaceAll("[.]", "/"))

        val confString = mergeConfigString(
            DirectTypeExtensibilityConfig.configureExtensibleTypes(forcedExtensibleTyptes),
            ClosedPackagesConfig.openCodeBase
        )

        val config = ConfigFactory.parseString(confString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(DirectTypeExtensibilityKey)

        println(project.config.getConfig("org.opalj.br.analyses"))

        it("it should be extensible, no matter what") {
            isExtensible(PublicFinalClass) should be(Yes)
            isExtensible(PublicClassWithPrivConstructors) should be(Yes)
        }
    }

    describe("when a type is configured as final") {

        val forcedExtensibleTyptes = List(PublicClass, PublicInterface).
            map(_.fqn.replaceAll("[.]", "/"))

        val confString = mergeConfigString(
            DirectTypeExtensibilityConfig.configureFinalTypes(forcedExtensibleTyptes),
            ClosedPackagesConfig.closedCodeBase
        )

        val config = ConfigFactory.parseString(confString)
        val project = Project.recreate(testProject, config, true)
        val isExtensible = project.get(DirectTypeExtensibilityKey)

        println(project.config.getConfig("org.opalj.br.analyses"))

        it("it should NOT be extensible, no matter what") {
            isExtensible(PublicClass) should be(No)
            isExtensible(PublicInterface) should be(No)
        }
    }
}

object DirectTypeExtensibilityConfig {

    val directTypeExtensibilityAnalysis = """
      |org.opalj.br.analyses.DirectTypeExtensibilityKey {
      |        extensibilityAnalysis = "org.opalj.br.analyses.DirectTypeExtensibilityInformation"
      |      }
    """.stripMargin

    def configureExtensibleTypes(types: List[String] = List.empty) = s"""
                            |org.opalj.br.analyses.DirectTypeExtensibilityKey {
                            |        extensibilityAnalysis = "org.opalj.br.analyses.ConfigureExtensibleTypes"
                            |        extensibleTypes = [${types.map("\""+_+"\"").mkString(",")}]
                            |      }
                          """.stripMargin

    def configureFinalTypes(types: List[String] = List.empty) = s"""
                               |org.opalj.br.analyses.DirectTypeExtensibilityKey {
                               |        extensibilityAnalysis = "org.opalj.br.analyses.ConfigureFinalTypes"
                               |        finalTypes = [${types.map("\""+_+"\"").mkString(",")}]
                               |      }
                             """.stripMargin

}

object ClosedPackagesConfig {

    val openCodeBase =
        """
        |org.opalj.br.analyses.ClosedPackagesKey {
        |         packageContext = "org.opalj.br.analyses.OpenCodeBase"
        |     }
      """.stripMargin

    val closedCodeBase = """
      |org.opalj.br.analyses.ClosedPackagesKey {
      |        packageContext = "org.opalj.br.analyses.ClosedCodeBase"
      |      }
    """.stripMargin

    def configureClosedPackages(regex: String = "java(/.*)") = s"""
      |org.opalj.br.analyses.ClosedPackagesKey {
      |        packageContext = "org.opalj.br.analyses.ClosedPackagesConfiguration"
      |        closedPackages = "$regex"
      |      }
    """.stripMargin

    def configureOpenPackages(regex: String = ".*") = s"""
      |org.opalj.br.analyses.ClosedPackagesKey {
      |        packageContext = "org.opalj.br.analyses.ClosedPackagesConfiguration"
      |        openPackages = "$regex"
      |      }
    """.stripMargin
}