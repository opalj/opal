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

import java.net.URL
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.PrintStream

import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bytecode.RTJar
import org.opalj.br.analyses.Project
import org.opalj.support.tools.ProjectBasedInMemoryClassLoader

/**
 * Test if OPAL is able to rewrite a simple lambda expression and check if the rewritten bytecode
 * is executable.
 *
 * @author Andreas Muttscheller
 */
class InvokedynamicRewritingTest extends FunSpec with Matchers {

    def FixtureProject(fixtureFiles: File): Project[URL] = {
        val classFileReader = Project.JavaClassFileReader()
        import classFileReader.ClassFiles
        val projectClassFiles = ClassFiles(fixtureFiles)
        val libraryClassFiles = ClassFiles(RTJar)

        info(s"the test fixture project consists of ${projectClassFiles.size} class files")
        Project(
            projectClassFiles,
            libraryClassFiles,
            libraryClassFilesAreInterfacesOnly = false
        )
    }

    describe("bi lambda fixtures") {
        it("should calculate 2+2 correctly") {
            val r = locateTestResources("lambdas-1.8-g-parameters-genericsignature.jar", "bi")
            val p = FixtureProject(r)
            val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p)
            val c = inMemoryClassLoader.loadClass("lambdas.InvokeDynamics")
            val instance = c.newInstance()
            val m = c.getMethod("simpleLambdaAdd", Integer.TYPE, Integer.TYPE)
            val res = m.invoke(instance, new Integer(2), new Integer(2))

            assert(res.asInstanceOf[Integer] == 4)
        }

        it("should serialize and deserialize lambdas properly") {
            val r = locateTestResources("lambdas-1.8-g-parameters-genericsignature.jar", "bi")
            val p = FixtureProject(r)
            val inMemoryClassLoader = new ProjectBasedInMemoryClassLoader(p)
            val c = inMemoryClassLoader.loadClass("lambdas.methodreferences.IntersectionTypes")
            val m = c.getMethod("serializedLambda")
            val res = m.invoke(null)

            assert(res.asInstanceOf[String] == "Hello World 23.14foo")
        }
    }

    describe("JCG lambda_expressions test") {
        it("should execute main successfully") {
            val p = FixtureProject(locateTestResources("classfiles/lambda_expressions.jar", "bi"))
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
}
