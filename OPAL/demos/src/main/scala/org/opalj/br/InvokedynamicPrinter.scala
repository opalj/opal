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
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.reader.LambdaExpressionsRewriting.{defaultConfig ⇒ lambdasRewritingConfig}

/**
 * Prints out the immediately available information about invokedynamic instructions.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object InvokedynamicPrinter extends DefaultOneStepAnalysis {

    // We have to adapt the configuration to ensure that invokedynamic instructions
    // are never rewritten!
    override def setupProject(
        cpFiles:                 Iterable[File],
        libcpFiles:              Iterable[File],
        completelyLoadLibraries: Boolean,
        projectType:             ProjectType,
        fallbackConfiguration:   Config
    )(
        implicit
        initialLogContext: LogContext
    ): Project[URL] = {
        val baseConfig = lambdasRewritingConfig(rewrite = false, logRewrites = true)
        val config = baseConfig.withFallback(fallbackConfiguration)
        super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, projectType, config)
    }

    override def description: String = "Prints information about invokedynamic instructions."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val invokedynamics = new ConcurrentLinkedQueue[String]()
        project.parForeachMethodWithBody(isInterrupted) { mi ⇒
            val method = mi.method
            val classFile = method.classFile
            val body = method.body.get
            invokedynamics.addAll(
                body.collectWithIndex {
                    case PCAndInstruction(pc, INVOKEDYNAMIC(bootstrap, name, descriptor)) ⇒
                        classFile.thisType.toJava+" {\n  "+method.signatureToJava()+"{ "+pc+": \n"+
                            s"    ${bootstrap.toJava}\n"+
                            bootstrap.arguments.mkString("    Arguments: {", ",", "}\n") +
                            s"    Calling:   ${descriptor.toJava(name)}\n"+
                            "} }\n"
                }.toList.asJava
            )
        }
        val result = invokedynamics.asScala.toSeq.sorted
        BasicReport(result.mkString(result.size+" invokedynamic instructions found:\n", "\n", "\n"))
    }

}
