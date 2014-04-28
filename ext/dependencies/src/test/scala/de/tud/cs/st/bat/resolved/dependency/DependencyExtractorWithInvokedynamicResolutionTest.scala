/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st
package bat
package resolved
package dependency

import analyses.Project
import ai.invokedynamic._
import instructions.INVOKEDYNAMIC
import reader.Java7Framework.ClassFiles
import DependencyType._
import org.scalatest.FunSuite
import java.net.URL

/**
 * Tests that the dependency extractor with support for invokedynamic does not miss some
 * dependencies and that it does not extract "unexpected" dependencies.
 *
 * @author Arne Lottmann
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DependencyExtractorWithInvokedynamicResolutionTest extends FunSuite {

    test("Dependency extraction") {
        var dependencies: Map[(String, String, DependencyType), Int] =
            DependencyExtractorFixture.extractDependencies(
                "ext/dependencies",
                "classfiles/invokedynamic_dependencies.jar")

        def assertDependency(src: String, trgt: String, dType: DependencyType): Unit = {
            val key = (src, trgt, dType)

            dependencies.get(key) match {
                case Some(0) ⇒
                    fail("The dependency "+key+" was not extracted the expected number of times.")
                case Some(x) ⇒
                    dependencies = dependencies.updated(key, x - 1)
                case None ⇒
                    val remainigDependencies =
                        dependencies.toList.sorted.
                            mkString("Remaining dependencies:\n\t", "\n\t", "\n")
                    fail("The dependency "+key+" was not extracted.\n"+remainigDependencies)
            }
        }

        assertDependency(
            "dependencies.SameClassDependencies.noArgumentsMethod()",
            "dependencies.SameClassDependencies",
            HAS_LOCAL_VARIABLE_OF_TYPE)
        assertDependency(
            "dependencies.SameClassDependencies.noArgumentsMethod()",
            "dependencies.SameClassDependencies",
            IS_INSTANCE_MEMBER)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies",
            HAS_LOCAL_VARIABLE_OF_TYPE)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies",
            IS_INSTANCE_MEMBER)

        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "org.codehaus.groovy.vmplugin.v7.IndyInterface",
            USES_METHOD_DECLARING_TYPE)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "org.codehaus.groovy.vmplugin.v7.IndyInterface.bootstrap(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.String, int)",
            CALLS_METHOD)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies",
            USES_PARAMETER_TYPE)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "java.lang.Object",
            USES_RETURN_TYPE)

        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies.noArgumentsMethod()",
            CALLS_METHOD)

        // TODO we need a good way to filter out all of groovy's automatically generated methods
        // so we can easily see which dependencies we have missed as in DependencyExtractorTest
        (dependencies.keys filter { triple ⇒
            triple._1.contains("noArgumentsMethod()") || triple._1.contains("dependencies()")
        }).foreach(println)
    }
}

