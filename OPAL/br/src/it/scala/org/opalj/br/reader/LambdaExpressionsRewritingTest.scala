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

import scala.language.existentials

import scala.collection.JavaConverters._

import org.scalatest.FunSuite
import java.lang.{Boolean ⇒ JBoolean}
import java.util.concurrent.ConcurrentLinkedQueue

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.opalj.log.StandardLogContext
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.analyses.SomeProject

/**
 * Infrastructure to load a project containing Jars.
 *
 * @author Arne Lottmann
 */
abstract class LambdaExpressionsRewritingTest extends FunSuite {

    protected def isProxyFactoryCall(instruction: INVOKESTATIC): Boolean = {
        isProxyFactoryCall(instruction.declaringClass.fqn)
    }

    protected def isProxyFactoryCall(declaringClassFQN: String): Boolean = {
        declaringClassFQN.matches(LambdaExpressionsRewriting.LambdaNameRegEx)
    }

    protected def proxyFactoryCalls(project: SomeProject): Iterable[INVOKESTATIC] = {
        val factoryCalls = new ConcurrentLinkedQueue[INVOKESTATIC]()
        project.parForeachMethodWithBody() { mi ⇒
            factoryCalls.addAll(
                (mi.method.body.get.collectInstructions {
                    case i: INVOKESTATIC if isProxyFactoryCall(i) ⇒ i
                }).asJava
            )
            /*
            for {
                (_,i @ INVOKESTATIC(declaringClass,_,_,_)) <- mi.method.body.get
                if isProxyFactoryCall(declaringClass.fqn)
            } {
                factoryCalls.add(i)
            }
            */
        }
        info(s"found ${factoryCalls.size} lambda proxy factory method calls")
        factoryCalls.asScala

    }

    /**
     * Loads the library and checks if at least one call to a proxy factory method is found.
     */
    protected def project(libraryPath: java.io.File): (SomeProject, Iterable[INVOKESTATIC]) = {
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = LambdaExpressionsRewriting.LambdaExpressionsRewritingConfigKey
        val logRewritingsConfigKey = LambdaExpressionsRewriting.LambdaExpressionsLogRewritingsConfigKey
        val config = baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.TRUE)).
            withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.FALSE)) /*.
            withValue(SynthesizedClassFiles., ConfigValueFactory.fromAnyRef(JBoolean.FALSE))
            */

        val logContext = new StandardLogContext
        OPALLogger.register(logContext)
        val project = Project(libraryPath, logContext, config)
        val proxyFactoryCalls = this.proxyFactoryCalls(project)
        assert(proxyFactoryCalls.nonEmpty, "there should be calls to the proxy factories")

        (project, proxyFactoryCalls)
    }

    protected def checkForMissingProxyClassFiles(
        project:           SomeProject,
        proxyFactoryCalls: Iterable[INVOKESTATIC]
    ): Unit = {
        val missingProxyClassFiles = for {
            proxyFactoryCall ← proxyFactoryCalls
            proxy = project.classFile(proxyFactoryCall.declaringClass)
            if proxy.isEmpty
        } yield {
            (proxy, proxyFactoryCall)
        }

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

    protected def load(libraryPath: java.io.File): SomeProject = {
        val (project, proxyFactoryCalls) = this.project(libraryPath)
        checkForMissingProxyClassFiles(project, proxyFactoryCalls)
        project
    }
}
