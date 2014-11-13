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
package br
package reader

import analyses.{ Project, SomeProject }
import instructions._
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * This test loads all classes found in the JRE and verifies that all [[INVOKEDYNAMIC]]
 * instructions can be resolved.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class JRELambdaResolutionTest extends FunSpec with Matchers {
    describe("The Java8FrameworkWithLambdaSupport") {
        describe("should resolve all invokedynamic instructions found in the JRE") {
            val jrePath = util.JRELibraryFolder
            val jreProject = Project(
                Java8FrameworkWithLambdaSupport.ClassFiles(jrePath),
                Traversable.empty
            )
            it("there should be no more invokedynamic instructions in the loaded class files") {
                val invokedynamics = (jreProject.methods.collect {
                    case MethodWithBody(body) ⇒ body.instructions.filter(_.isInstanceOf[INVOKEDYNAMIC])
                }).flatten
                invokedynamics should be('empty)
            }
            
            def isProxyFactoryCall(instruction: INVOKESTATIC): Boolean =
            		instruction.declaringClass.fqn.matches("^Lambda\\$\\d+:\\d+$")
            		
            it("but there should be calls to proxy factories (on JDK8 only)") {
                val invokestatics: Iterable[INVOKESTATIC] = (jreProject.methods.collect {
                    case MethodWithBody(body) ⇒ body.instructions.filter(
                            _.isInstanceOf[INVOKESTATIC]).map(_.asInstanceOf[INVOKESTATIC])
                }).flatten
                val proxyFactoryCalls = invokestatics.filter(isProxyFactoryCall)
				if (System.getProperty("java.version").startsWith("1.8"))
	                proxyFactoryCalls should not be ('empty)
            }
            it("and a proxy class file for each call") {
                val missingProxyClassFiles = (for {
                    classFile ← jreProject.classFiles
                    method @ MethodWithBody(body) ← classFile.methods
                    proxyFactoryCall ← body.instructions.collect { case x: INVOKESTATIC => x }
                    if isProxyFactoryCall(proxyFactoryCall)
                    proxy = jreProject.classFile(proxyFactoryCall.declaringClass)
                    if !proxy.isDefined
                } yield {
                    (classFile, method, proxyFactoryCall)
                })
                if (missingProxyClassFiles.nonEmpty) {
                    val totalFailures = missingProxyClassFiles.size
                    val data = missingProxyClassFiles.mkString(
                        "Missing proxy ClassFiles for the following instructions:\n\n",
                        "\n\n",
                        "")
                    val logFile = util.writeAndOpen(data, "MissingProxyClassFiles", ".txt")
                    val msg = s"Missing $totalFailures proxy ClassFiles for lambdas!\nSee $logFile for details."
                    fail(msg)
                }
            }
        }
    }
}
