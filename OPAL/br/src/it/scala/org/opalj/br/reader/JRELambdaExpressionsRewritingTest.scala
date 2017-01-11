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
package reader

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.bi.isCurrentJREAtLeastJava8
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.DefaultLogContext
import org.opalj.log.OPALLogger

/**
 * This test loads all classes found in the JRE and verifies that all [[INVOKEDYNAMIC]]
 * instructions can be resolved.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class JRELambdaExpressionsRewritingTest extends FunSpec with Matchers {

    describe("the Java8LambdaExpressionsRewriting framework") {

        describe("should rewrite all invokedynamic instructions found in the JRE") {
            val jrePath = org.opalj.bytecode.JRELibraryFolder
            val baseConfig: Config = ConfigFactory.load()
            val rewritingConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsRewritingConfigKey
            val logRewritingsConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsLogRewritingsConfigKey
            val config = baseConfig.
                withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.TRUE)).
                withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE)) /*.
                withValue(SynthesizedClassFiles., ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE))*/

            val logContext = new DefaultLogContext
            OPALLogger.register(logContext)
            val jreProject = Project(jrePath, logContext, config)

            it("there should be no more invokedynamic instructions in the loaded class files") {
                val invokedynamics = (jreProject.allMethodsWithBody.map { method ⇒
                    method.body.get.collect { case i: INVOKEDYNAMIC ⇒ i }
                }).flatten
                // if the test fails we want to know the invokedynamic instructions
                invokedynamics should be('empty)
            }

            if (isCurrentJREAtLeastJava8) {

                def isProxyFactoryCall(instruction: INVOKESTATIC): Boolean = {
                    instruction.declaringClass.fqn.matches("^Lambda\\$\\d+:\\d+$")
                }

                it("but there should be calls to proxy factories (on JDK8 only)") {
                    val invokestatics: Iterable[INVOKESTATIC] = (jreProject.allMethodsWithBody.map { method ⇒
                        method.body.get.collectInstructions { case i: INVOKESTATIC ⇒ i }
                    }).flatten
                    val proxyFactoryCalls = invokestatics.filter(isProxyFactoryCall)
                    proxyFactoryCalls should not be ('empty)
                }

                it("and a proxy class file for each call") {

                    val missingProxyClassFiles =
                        for {
                            classFile ← jreProject.allProjectClassFiles.par
                            method @ MethodWithBody(body) ← classFile.methods
                            proxyFactoryCall ← body.instructions.collect { case i: INVOKESTATIC ⇒ i }
                            if isProxyFactoryCall(proxyFactoryCall)
                            proxy = jreProject.classFile(proxyFactoryCall.declaringClass)
                            if proxy.isEmpty
                        } yield {
                            (classFile, method, proxyFactoryCall)
                        }

                    //  proxyFactoryCallsCount

                    if (missingProxyClassFiles.nonEmpty) {
                        val totalFailures = missingProxyClassFiles.size
                        val data = missingProxyClassFiles.mkString(
                            "Missing proxy ClassFiles for the following instructions:\n\n",
                            "\n\n",
                            ""
                        )
                        val logFile = io.writeAndOpen(data, "MissingProxyClassFiles", ".txt")
                        val msg = s"Missing $totalFailures proxy ClassFiles for lambdas!\nSee $logFile for details."
                        fail(msg)
                    }
                }
            } else {
                info("the current JDK/JRE does not make use of invokedynamic or was not correctly recognized")
            }
        }
    }
}
