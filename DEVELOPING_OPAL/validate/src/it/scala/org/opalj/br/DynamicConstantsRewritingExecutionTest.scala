/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.math.RoundingMode
import java.net.URL

import com.typesafe.config.Config
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bytecode.RTJar
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.isCurrentJREAtLeastJava11
import org.opalj.bi.isCurrentJREAtLeastJava15
import org.opalj.br.analyses.Project
import org.opalj.br.reader.DynamicConstantRewriting
import org.opalj.ba.ProjectBasedInMemoryClassLoader

/**
 * Tests if OPAL is able to rewrite dynamic constants and checks if the rewritten bytecode is
 * executable and produces correct results.
 *
 * @author Dominik Helm
 */
class DynamicConstantsRewritingExecutionTest extends AnyFunSpec with Matchers {

    def JavaFixtureProject(fixtureFiles: File): Project[URL] = {
        implicit val config: Config = DynamicConstantRewriting.defaultConfig(
            rewrite = true,
            logRewrites = false
        )

        val projectClassFiles = Project.JavaClassFileReader.ClassFiles(fixtureFiles)
        val libraryClassFiles = Project.JavaLibraryClassFileReader.ClassFiles(RTJar)

        Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false
        )
    }

    if (isCurrentJREAtLeastJava11) {
        describe("behavior of rewritten dynamic constants test project") {
            import java.lang.invoke.VarHandle

            // Note: The bytecode for this test project was created using
            // [[org.opalj.test.fixtures.dynamicConstants.DynamicConstantsCreationTest]]
            val dynamicConstantsJar = locateTestResources("classfiles/dynamic_constants.jar", "bi")
            val project = JavaFixtureProject(dynamicConstantsJar)
            val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(project)
            val testClass = inMemoryClassLoader.loadClass("Test")

            it("should get an array VarHandle correctly") {
                val m = testClass.getMethod("arrayVarHandle")
                val res = m.invoke(null).asInstanceOf[VarHandle]

                val expectedTypes = Array(classOf[Array[String]], classOf[Int])
                assert(res.coordinateTypes().toArray() sameElements expectedTypes)
            }

            it("should get an enum constant correctly") {
                val m = testClass.getMethod("enumConstant")
                val res = m.invoke(null)

                assert(res == RoundingMode.DOWN)
            }

            it("should get a field VarHandle correctly") {
                val m = testClass.getMethod("fieldVarHandle")
                val res = m.invoke(null).asInstanceOf[VarHandle]

                assert(res.coordinateTypes().toArray() sameElements Array(testClass))
                assert(res.varType() == testClass)
            }

            it("should get a static final singleton field correctly") {
                val m = testClass.getMethod("getStaticFinal1")
                val res = m.invoke(null)

                assert(res eq testClass.getField("singletonField").get(null))
            }

            it("should get a static final field correctly") {
                val m = testClass.getMethod("getStaticFinal2")
                val res = m.invoke(null)

                assert(res == testClass.getField("staticField").get(null))
            }

            it("should perform an invocation correctly") {
                val m = testClass.getMethod("invoke")
                val res = m.invoke(null)

                assert(res == 1337)
            }

            it("should get a null constant correctly") {
                val m = testClass.getMethod("nullConstant")
                val res = m.invoke(null)

                assert(m.getReturnType == testClass)
                assert(res eq null)
            }

            it("should get a primitive class correctly") {
                val m = testClass.getMethod("primitiveClass")
                val res = m.invoke(null)

                assert(res eq classOf[Int])
            }

            it("should get a static field VarHandle correctly") {
                val m = testClass.getMethod("staticFieldVarHandle")
                val res = m.invoke(null).asInstanceOf[VarHandle]

                assert(res.coordinateTypes().isEmpty)
                assert(res.varType() == testClass)
            }

            it("should work correctly with nested constants") {
                val m = testClass.getMethod("nestedConstants1")
                val res = m.invoke(null)

                assert(res == 1337)
            }

            if (isCurrentJREAtLeastJava15) {
                it("should perform an explicit cast correctly") {
                    val m = testClass.getMethod("explicitCast")
                    m.invoke(null)

                    // Can't test much here, but ensure the invocation doesn't fail and the return
                    // type is as expected
                    assert(m.getReturnType == classOf[Object])
                }

                it("should work correctly with nested constants including explicitCast") {
                    val m = testClass.getMethod("nestedConstants2")
                    val res = m.invoke(null)

                    assert(m.getReturnType == classOf[Long])
                    assert(res == 1337L)
                }
            }
        }
    }
}
