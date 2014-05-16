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
package org.opalj
package de

import org.scalatest.FunSuite

import br._
import br.instructions.INVOKEDYNAMIC
import ai.invokedynamic._


/**
 * Tests that the dependency extractor with support for invokedynamic does not miss some
 * dependencies and that it does not extract "unexpected" dependencies.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DependencyExtractorWithInvokedynamicResolutionTest extends FunSuite {

    test("Dependency extraction") {
        import DependencyType._

        var dependencies: Map[(String, String, DependencyType), Int] =
            DependencyExtractorFixture.extractDependencies(
                "de",
                "classfiles/invokedynamic_dependencies.jar",
                (dp: DependencyProcessor) ⇒ {
                    new DependencyExtractorWithInvokedynamicResolution(
                        dp,
                        new InvokedynamicResolver {
                            override def resolveInvokedynamic(
                                instruction: INVOKEDYNAMIC): ResolutionResult =
                                ResolutionFailed(instruction)
                        }
                    )
                }
            )

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
            LOCAL_VARIABLE_TYPE)
        assertDependency(
            "dependencies.SameClassDependencies.noArgumentsMethod()",
            "dependencies.SameClassDependencies",
            INSTANCE_MEMBER)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies",
            LOCAL_VARIABLE_TYPE)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies",
            INSTANCE_MEMBER)

        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "org.codehaus.groovy.vmplugin.v7.IndyInterface",
            DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "org.codehaus.groovy.vmplugin.v7.IndyInterface.bootstrap(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.String, int)",
            CALLS_METHOD)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "dependencies.SameClassDependencies",
            PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency(
            "dependencies.SameClassDependencies.dependencies()",
            "java.lang.Object",
            RETURN_TYPE_OF_CALLED_METHOD)

        /* THE DEFAULT STRATEGY CANNOT EXTRACT THE UNDERLYING CALL!
         * assertDependency(
         * "dependencies.SameClassDependencies.dependencies()",
         * "dependencies.SameClassDependencies.noArgumentsMethod()",
         * CALLS_METHOD)
        */
    }
}

