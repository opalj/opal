/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.net.URL
import java.net.URLClassLoader
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

import com.typesafe.config.Config

import org.opalj.bytecode.RTJar
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.isCurrentJREAtLeastJava10
import org.opalj.bi.isCurrentJREAtLeastJava16
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
class InvokedynamicRewritingExecutionTest extends AnyFunSpec with Matchers {

    def JavaFixtureProject(fixtureFiles: File): Project[URL] = {
        implicit val config: Config = InvokedynamicRewriting.defaultConfig(
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

    private def validateMethod(
        expectedResult:            AnyRef,
        testClassLoader:           ClassLoader,
        fixtureClassLoader:        ClassLoader,
        testClassType:             ObjectType,
        testMethodName:            String,
        testMethodParameterTypes:  Array[Class[_]]         = Array.empty,
        testMethodParameters:      Array[AnyRef]           = Array.empty,
        constructorParameterTypes: Option[Array[Class[_]]] = None,
        constructorParameters:     Option[Array[AnyRef]]   = None
    ): Unit = {

        def getResult(cl: ClassLoader): AnyRef = {
            val clazz = cl.loadClass(testClassType.toJava)
            val instance = if (constructorParameterTypes.isDefined) {
                val ctor = clazz.getConstructor(constructorParameterTypes.get: _*)
                ctor.newInstance(constructorParameters.get: _*)
            } else null
            val method = clazz.getMethod(testMethodName, testMethodParameterTypes: _*)
            method.invoke(instance, testMethodParameters: _*)
        }

        val testResult = getResult(testClassLoader)
        val fixtureResult = getResult(fixtureClassLoader)
        assert(testResult == fixtureResult)
        assert(testResult == expectedResult)
    }

    describe("behavior of rewritten bi.lambdas fixtures") {
        val r = locateTestResources("lambdas-1.8-g-parameters-genericsignature.jar", "bi")
        val p = JavaFixtureProject(r)
        val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p)
        val fixtureClassLoader = new URLClassLoader(Array(r.toURI.toURL))

        val intersectionTypes = ObjectType("lambdas/methodreferences/IntersectionTypes")

        it("simpleLambdaAdd should calculate 2+2 correctly") {
            validateMethod(
                Int.box(4),
                inMemoryClassLoader,
                fixtureClassLoader,
                ObjectType("lambdas/InvokeDynamics"),
                "simpleLambdaAdd",
                Array(classOf[Int], classOf[Int]),
                Array(Int.box(2), Int.box(2)),
                Some(Array.empty), Some(Array.empty)
            )
        }

        it("serializedLambda should work with only objects properly") {
            validateMethod(
                "Hello World 23.14foo",
                inMemoryClassLoader,
                fixtureClassLoader,
                intersectionTypes,
                "lambdaWithObjectCaptures"
            )
        }

        it("serializedLambda should work with object and primitives properly") {
            validateMethod(
                "Hello World 23.14foo",
                inMemoryClassLoader,
                fixtureClassLoader,
                intersectionTypes,
                "lambdaWithObjectAndPrimitiveCaptures"
            )
        }

        it("serializedLambda should work with object array properly") {
            validateMethod(
                "Hello World 3.1442.0",
                inMemoryClassLoader,
                fixtureClassLoader,
                intersectionTypes,
                "lambdaWithPrimitiveArray"
            )
        }

        it("serializedLambda should work with primitive array properly") {
            validateMethod(
                "Hello World 3.1442.0",
                inMemoryClassLoader,
                fixtureClassLoader,
                intersectionTypes,
                "lambdaWithPrimitiveArray"
            )
        }

        it("serializedLambda should work with primitive array and object properly") {
            validateMethod(
                "Hello World 3.1442.0foo",
                inMemoryClassLoader,
                fixtureClassLoader,
                intersectionTypes,
                "lambdaWithPrimitiveArrayAndObject"
            )
        }
    }

    describe("behavior of rewritten JCG lambda_expressions project") {

        it("should execute main successfully") {
            val r = locateTestResources("classfiles/jcg_lambda_expressions.jar", "bi")
            val p = JavaFixtureProject(r)
            val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p)
            val fixtureClassLoader = new URLClassLoader(Array(r.toURI.toURL))

            val testClass = inMemoryClassLoader.loadClass("app.ExpressionPrinter")
            val testMethod = testClass.getMethod("main", classOf[Array[String]])

            val fixtureClass = fixtureClassLoader.loadClass("app.ExpressionPrinter")
            val fixtureMethod = fixtureClass.getMethod("main", classOf[Array[String]])

            // Intercept output
            val defaultOut = System.out
            val testBaos = new ByteArrayOutputStream()
            System.setOut(new PrintStream(testBaos))

            testMethod.invoke(null, Array("lambda_expressions.jar"))
            val testResult = testBaos.toString

            val fixtureBaos = new ByteArrayOutputStream()
            System.setOut(new PrintStream(fixtureBaos))

            fixtureMethod.invoke(null, Array("lambda_expressions.jar"))
            val fixtureResult = fixtureBaos.toString

            assert(testResult == fixtureResult)
            assert(testResult == "Id(((1)++)²)\n")

            // Reset System.out
            System.setOut(defaultOut)
        }
    }

    if (isCurrentJREAtLeastJava10) {

        describe("behavior of rewritten string_concat fixture") {
            val testClassType = ObjectType("string_concat/StringConcatFactoryTest")
            val r = locateTestResources("classfiles/string_concat.jar", "bi")
            val p = JavaFixtureProject(r)
            val cf = p.classFile(testClassType).get.copy(version = bi.Java8Version)
            val inMemoryClassLoader =
                new InMemoryClassLoader(Map(testClassType.toJava -> Assembler(ba.toDA(cf))))
            val fixtureClassLoader = new URLClassLoader(Array(r.toURI.toURL))

            it("simpleConcat should concatenate strings correctly") {
                validateMethod(
                    "abc",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "simpleConcat",
                    Array(classOf[String], classOf[String]),
                    Array("ab", "c")
                )
            }

            it("concatConstants should produce the correct result") {
                validateMethod(
                    "ab c5",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "concatConstants",
                    Array(classOf[String], classOf[String]),
                    Array("ab", "c")
                )
            }

            it("concatObjectAndInt should produce the correct result") {
                validateMethod(
                    "abList(1, c)5",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "concatObjectAndInt",
                    Array(classOf[String], classOf[Object], classOf[Int]),
                    Array("ab", List(1, "c"), Int.box(5))
                )
            }

            it("concatObjectAndDoubleWithConstants should produce the correct result") {
                validateMethod(
                    " 7.32.5List(ab, 1)",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "concatObjectAndDoubleWithConstants",
                    Array(classOf[Object], classOf[Double]),
                    Array(List("ab", 1), Double.box(7.3))
                )
            }

            it("concatLongAndConstant should produce the correct result") {
                validateMethod(
                    "ab157",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "concatLongAndConstant",
                    Array(classOf[Long], classOf[String]),
                    Array(Long.box(7L), "ab")
                )
            }

            it("concatClassConstant should produce the correct result.StringConcatFactoryTest") {
                validateMethod(
                    "abclass string_concat.StringConcatFactoryTest",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "concatClassConstant",
                    Array(classOf[String]),
                    Array("ab")
                )
            }

            it("concatNonInlineableConstant should produce the correct result") {
                validateMethod(
                    "ab\u0001\u0002",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "concatNonInlineableConstant",
                    Array(classOf[String]),
                    Array("ab")
                )
            }
        }
    }

    if (isCurrentJREAtLeastJava16) {

        describe("behavior of rewritten java16records fixture") {
            val testClassType = ObjectType("java16records/RecordClass")
            val r = locateTestResources("java16records-g-16-parameters-genericsignature.jar", "bi")
            val p = JavaFixtureProject(r)
            val cf = p.classFile(testClassType).get.copy(version = bi.Java8Version)
            val inMemoryClassLoader =
                new InMemoryClassLoader(Map(testClassType.toJava -> Assembler(ba.toDA(cf))))
            val fixtureClassLoader = new URLClassLoader(Array(r.toURI.toURL))

            it("should provide toString as expected") {
                validateMethod(
                    "RecordClass[component1=42, component2=foo]",
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "toString",
                    Array.empty, Array.empty,
                    Some(Array(classOf[Int], classOf[Object])),
                    Some(Array(Int.box(42), "foo"))
                )
            }

            it("should provide equals as expected") {
                val testClass = inMemoryClassLoader.loadClass(testClassType.toJava)
                val testCtor = testClass.getConstructor(classOf[Int], classOf[Object])
                val testRecord = testCtor.newInstance(Int.box(42), "foo").asInstanceOf[AnyRef]
                val testEqualRecord = testCtor.newInstance(Int.box(42), "foo").asInstanceOf[AnyRef]
                val testDiffRecord1 = testCtor.newInstance(Int.box(21), "foo").asInstanceOf[AnyRef]
                val testDiffRecord2 = testCtor.newInstance(Int.box(42), "bar").asInstanceOf[AnyRef]
                val testMethod = testClass.getMethod("equals", classOf[Object])

                val fixtureClass = fixtureClassLoader.loadClass(testClassType.toJava)
                val fixtureCtor = fixtureClass.getConstructor(classOf[Int], classOf[Object])
                val fixtureRecord = fixtureCtor.newInstance(Int.box(42), "foo").asInstanceOf[AnyRef]
                val fixtureEqualRecord =
                    fixtureCtor.newInstance(Int.box(42), "foo").asInstanceOf[AnyRef]
                val fixtureDiffRecord1 =
                    fixtureCtor.newInstance(Int.box(21), "foo").asInstanceOf[AnyRef]
                val fixtureDiffRecord2 =
                    fixtureCtor.newInstance(Int.box(42), "bar").asInstanceOf[AnyRef]
                val fixtureMethod = fixtureClass.getMethod("equals", classOf[Object])

                val testEqRes = testMethod.invoke(testRecord, testEqualRecord)
                val fixtureEqRes = fixtureMethod.invoke(fixtureRecord, fixtureEqualRecord)
                assert(testEqRes == fixtureEqRes)
                assert(testEqRes.asInstanceOf[Boolean])

                val testDiffRes1 = testMethod.invoke(testRecord, testDiffRecord1)
                val fixtureDiffRes1 = fixtureMethod.invoke(fixtureRecord, fixtureDiffRecord1)
                assert(testDiffRes1 == fixtureDiffRes1)
                assert(!testDiffRes1.asInstanceOf[Boolean])

                val testDiffRes2 = testMethod.invoke(testRecord, testDiffRecord2)
                val fixtureDiffRes2 = fixtureMethod.invoke(fixtureRecord, fixtureDiffRecord2)
                assert(testDiffRes2 == fixtureDiffRes2)
                assert(!testDiffRes2.asInstanceOf[Boolean])
            }

            it("should provide hashCode as expected") {
                validateMethod(
                    Int.box(Integer.hashCode(42) * 31+"foo".hashCode),
                    inMemoryClassLoader,
                    fixtureClassLoader,
                    testClassType,
                    "hashCode",
                    Array.empty, Array.empty,
                    Some(Array(classOf[Int], classOf[Object])),
                    Some(Array(Int.box(42), "foo"))
                )
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
