/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.FunSpec
import org.scalatest.Matchers
import java.net.URL
import java.net.URLClassLoader
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

import org.opalj.bytecode.RTJar
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.isCurrentJREAtLeastJava9
import org.opalj.br.analyses.Project
import org.opalj.br.reader.InvokedynamicRewriting
import org.opalj.ba.ProjectBasedInMemoryClassLoader
import org.opalj.bc.Assembler
import org.opalj.io.JARsFileFilter
import org.opalj.util.InMemoryClassLoader

/**
 * Tests if OPAL is able to rewrite invokedynamics and checks if the rewritten bytecode is
 * executable and produces correct results.
 *
 * @author Andreas Muttscheller
 * @author Dominik Helm
 */
class InvokedynamicRewritingExecutionTest extends FunSpec with Matchers {

    def JavaFixtureProject(fixtureFiles: File): Project[URL] = {
        implicit val config = InvokedynamicRewriting.defaultConfig(
            rewrite = true,
            logRewrites = false
        )

        val projectClassFiles = Project.JavaClassFileReader.ClassFiles(fixtureFiles)
        val libraryClassFiles = Project.JavaLibraryClassFileReader.ClassFiles(RTJar)

        info(s"the test fixture project consists of ${projectClassFiles.size} class files")
        Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false
        )
    }

    describe("behavior of rewritten bi.lambdas fixtures") {
        val r = locateTestResources("lambdas-1.8-g-parameters-genericsignature.jar", "bi")
        val p = JavaFixtureProject(r)
        val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p)

        it("simpleLambdaAdd should calculate 2+2 correctly") {
            val c = inMemoryClassLoader.loadClass("lambdas.InvokeDynamics")
            val instance = c.getDeclaredConstructor().newInstance()
            val m = c.getMethod("simpleLambdaAdd", classOf[Int], classOf[Int])
            val res = m.invoke(instance, Int.box(2), Int.box(2))

            assert(res.asInstanceOf[Integer] == 4)
        }

        it("serializedLambda should work with only objects properly") {
            val c = inMemoryClassLoader.loadClass("lambdas.methodreferences.IntersectionTypes")
            val m = c.getMethod("lambdaWithObjectCaptures")
            val res = m.invoke(null)

            assert(res.asInstanceOf[String] == "Hello World 23.14foo")
        }

        it("serializedLambda should work with object and primitives properly") {
            val c = inMemoryClassLoader.loadClass("lambdas.methodreferences.IntersectionTypes")
            val m = c.getMethod("lambdaWithObjectAndPrimitiveCaptures")
            val res = m.invoke(null)

            assert(res.asInstanceOf[String] == "Hello World 23.14foo")
        }

        it("serializedLambda should work with object array properly") {
            val c = inMemoryClassLoader.loadClass("lambdas.methodreferences.IntersectionTypes")
            val m = c.getMethod("lambdaWithObjectArray")
            val res = m.invoke(null)

            assert(res.asInstanceOf[String] == "Hello World 3.1442.0")
        }

        it("serializedLambda should work with primitive array properly") {
            val c = inMemoryClassLoader.loadClass("lambdas.methodreferences.IntersectionTypes")
            val m = c.getMethod("lambdaWithPrimitiveArray")
            val res = m.invoke(null)

            assert(res.asInstanceOf[String] == "Hello World 3.1442.0")
        }

        it("serializedLambda should work with primitive array and object properly") {
            val c = inMemoryClassLoader.loadClass("lambdas.methodreferences.IntersectionTypes")
            val m = c.getMethod("lambdaWithPrimitiveArrayAndObject")
            val res = m.invoke(null)

            assert(res.asInstanceOf[String] == "Hello World 3.1442.0foo")
        }
    }

    describe("behavior of rewritten JCG lambda_expressions project") {

        it("should execute main successfully") {
            val p = JavaFixtureProject(
                locateTestResources("classfiles/jcg_lambda_expressions.jar", "bi")
            )
            val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p)

            val c = inMemoryClassLoader.loadClass("app.ExpressionPrinter")
            val m = c.getMethod("main", classOf[Array[String]])

            // Intercept output
            val baos = new ByteArrayOutputStream()
            val defaultOut = System.out
            System.setOut(new PrintStream(baos))

            m.invoke(null, Array("lambda_expressions.jar"))
            assert(baos.toString == "Id(((1)++)²)\n")

            // Reset System.out
            System.setOut(defaultOut)
        }
    }

    if (isCurrentJREAtLeastJava9) {

        describe("behavior of rewritten string_concat fixture") {
            val testClassType = ObjectType("string_concat/StringConcatFactoryTest")
            val r = locateTestResources("classfiles/string_concat.jar", "bi")
            val p = JavaFixtureProject(r)
            val cf = p.classFile(testClassType).get.copy(version = bi.Java8Version)
            val inMemoryClassLoader =
                new InMemoryClassLoader(Map(testClassType.toJava -> Assembler(ba.toDA(cf))))

            it("simpleConcat should concatenate strings correctly") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m = c.getMethod("simpleConcat", classOf[String], classOf[String])
                val res = m.invoke(null, "ab", "c")

                // Not using res.toString as that would not check that e.g. the StringBuilder is not
                // returned directly
                assert(res.asInstanceOf[String] == "abc")
            }

            it("concatConstants should produce the correct result") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m = c.getMethod("concatConstants", classOf[String], classOf[String])
                val res = m.invoke(null, "ab", "c")

                assert(res.asInstanceOf[String] == "ab c5")
            }

            it("concatObjectAndInt should produce the correct result") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m =
                    c.getMethod("concatObjectAndInt", classOf[String], classOf[Object], classOf[Int])
                val res = m.invoke(null, "ab", List(1, "c"), Int.box(5))

                assert(res.asInstanceOf[String] == "abList(1, c)5")
            }

            it("concatObjectAndDoubleWithConstants should produce the correct result") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m =
                    c.getMethod("concatObjectAndDoubleWithConstants", classOf[Object], classOf[Double])
                val res = m.invoke(null, List("ab", 1), Double.box(7.3))

                assert(res.asInstanceOf[String] == " 7.32.5List(ab, 1)")
            }

            it("concatLongAndConstant should produce the correct result") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m = c.getMethod("concatLongAndConstant", classOf[Long], classOf[String])
                val res = m.invoke(null, Long.box(7L), "ab")

                assert(res.asInstanceOf[String] == "ab157")
            }

            it("concatClassConstant should produce the correct result.StringConcatFactoryTest") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m = c.getMethod("concatClassConstant", classOf[String])
                val res = m.invoke(null, "ab")

                assert(res.asInstanceOf[String] == "abclass string_concat.StringConcatFactoryTest")
            }

            it("concatNonInlineableConstant should produce the correct result") {
                val c = inMemoryClassLoader.loadClass(testClassType.toJava)
                val m = c.getMethod("concatNonInlineableConstant", classOf[String])
                val res = m.invoke(null, "ab")

                assert(res.asInstanceOf[String] == "ab\u0001\u0002")
            }
        }
    }

    describe("behavior of rewritten OPAL") {

        it("should execute Hermes successfully") {
            val resources = locateTestResources("classfiles/OPAL-MultiJar-SNAPSHOT-01-04-2018.jar", "bi")
            val p = JavaFixtureProject(resources)
            val opalDependencies =
                locateTestResources(
                    "classfiles/OPAL-MultiJar-SNAPSHOT-01-04-2018-dependencies/", "bi"
                ).
                    listFiles(JARsFileFilter).
                    map(_.toURI.toURL)

            // Otherwise, the hermes resources are not included and hermes won't find
            // HermesCLI.txt for example
            val paths = Array(
                new File("TOOLS/hermes/src/main/resources/").toURI.toURL,
                new File("DEVELOPING_OPAL/tools/src/main/resources/").toURI.toURL,
                new File("OPAL/ai/src/main/resources/").toURI.toURL,
                new File("OPAL/ba/src/main/resources/").toURI.toURL,
                new File("OPAL/bi/src/main/resources/").toURI.toURL,
                new File("OPAL/bp/src/main/resources/").toURI.toURL,
                new File("OPAL/br/src/main/resources/").toURI.toURL,
                new File("OPAL/common/src/main/resources/").toURI.toURL
            ) ++ opalDependencies
            val resourceClassloader = new URLClassLoader(paths, this.getClass.getClassLoader)
            val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p, resourceClassloader)

            val c = inMemoryClassLoader.loadClass("org.opalj.hermes.HermesCLI")
            val m = c.getMethod("main", classOf[Array[String]])

            val tempFile = Files.createTempFile("OPALValidate-Hermes-stats-", ".csv").toFile
            tempFile.delete()

            info("Starting Hermes...")
            m.invoke(null, Array(
                "-config", "DEVELOPING_OPAL/validate/src/it/resources/hermes-test-fixtures.json",
                "-statistics", tempFile.getAbsolutePath
            ))

            assert(tempFile.exists())
            assert(tempFile.length() > 0)

            resourceClassloader.close()
        }
    }
}
