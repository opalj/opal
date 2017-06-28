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

import org.scalatest.FunSuite
import java.lang.{Boolean ⇒ JBoolean}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.DefaultLogContext
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.bi.isCurrentJREAtLeastJava8

/**
 * This test loads all classes found in the JRE and verifies that all [[INVOKEDYNAMIC]]
 * instructions can be resolved.
 *
 * @author Arne Lottmann
 */
class JRELambdaExpressionsRewritingTest extends FunSuite {

    if (!isCurrentJREAtLeastJava8) {
        fail("the current JDK does not use invokedynamic or was not correctly recognized")
    }

    test("rewriting of invokedynamic instructions in the JRE") {
        val jrePath = org.opalj.bytecode.JRELibraryFolder
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsRewritingConfigKey
        val logRewritingsConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsLogRewritingsConfigKey
        val config = baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.TRUE)).
            withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.FALSE)) /*.
                withValue(SynthesizedClassFiles., ConfigValueFactory.fromAnyRef(JBoolean.FALSE))*/

        val logContext = new DefaultLogContext
        OPALLogger.register(logContext)
        val jreProject = Project(jrePath, logContext, config)

        val invokedynamics = jreProject.allMethodsWithBody.flatMap { method ⇒
            method.body.get.collect { case i: INVOKEDYNAMIC ⇒ i }
        }
        // if the test fails we want to know the invokedynamic instructions
        assert(invokedynamics.isEmpty, "all invokedynamics should have been removed")

        def isProxyFactoryCall(instruction: INVOKESTATIC): Boolean = {
            instruction.declaringClass.fqn.matches("^Lambda\\$\\d+:\\d+$")
        }

        val invokestatics: Iterable[INVOKESTATIC] = jreProject.allMethodsWithBody.flatMap { method ⇒
            method.body.get.collectInstructions { case i: INVOKESTATIC ⇒ i }
        }
        val proxyFactoryCalls = invokestatics.filter(isProxyFactoryCall)
        assert(proxyFactoryCalls.nonEmpty, "there should be calls to the proxy factories")

        val missingProxyClassFiles =
            for {
                classFile ← jreProject.allProjectClassFiles.par
                method  ← classFile.methods
                body <- method.body.toSeq
                proxyFactoryCall ← body.instructions.collect { case i: INVOKESTATIC ⇒ i }
                if isProxyFactoryCall(proxyFactoryCall)
                proxy = jreProject.classFile(proxyFactoryCall.declaringClass)
                if proxy.isEmpty
            } yield {
                (classFile, method, proxyFactoryCall)
            }

        //  proxyFactoryCallsCount

        if (missingProxyClassFiles.nonEmpty) {
            val failures = missingProxyClassFiles.size
            val data = missingProxyClassFiles.mkString(
                "missing proxy ClassFiles for the following instructions:\n\t", "\n\t", "\n"
            )
            val logFile = io.writeAndOpen(data, "MissingProxyClassFiles", ".txt")
            val msg = s"missing $failures proxy ClassFiles for lambdas; see $logFile for details"
            fail(msg)
        }

    }

}
