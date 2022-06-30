/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._

import com.typesafe.config.Config

import org.opalj.log.LogContext
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.reader.InvokedynamicRewriting.{defaultConfig => invokedynamicRewritingConfig}

/**
 * Prints out the immediately available information about invokedynamic instructions.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object InvokedynamicPrinter extends ProjectAnalysisApplication {

    // We have to adapt the configuration to ensure that invokedynamic instructions
    // are never rewritten!
    override def setupProject(
        cpFiles:                 Iterable[File],
        libcpFiles:              Iterable[File],
        completelyLoadLibraries: Boolean,
        fallbackConfiguration:   Config
    )(
        implicit
        initialLogContext: LogContext
    ): Project[URL] = {
        val baseConfig = invokedynamicRewritingConfig(rewrite = false, logRewrites = true)
        val config = baseConfig.withFallback(fallbackConfiguration)
        super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries, config)
    }

    override def description: String = "Prints information about invokedynamic instructions."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val invokedynamics = new ConcurrentLinkedQueue[String]()
        project.parForeachMethodWithBody(isInterrupted) { mi =>
            val method = mi.method
            val classFile = method.classFile
            val body = method.body.get
            invokedynamics.addAll(
                body.collectWithIndex {
                    case PCAndInstruction(pc, INVOKEDYNAMIC(bootstrap, name, descriptor)) =>
                        classFile.thisType.toJava+" {\n  "+method.signatureToJava()+"{ "+pc+": \n"+
                            s"    ${bootstrap.toJava}\n"+
                            bootstrap.arguments.mkString("    Arguments: {", ",", "}\n") +
                            s"    Calling:   ${descriptor.toJava(name)}\n"+
                            "} }\n"
                }.toList.asJava
            )
        }
        val result = invokedynamics.asScala.toSeq.sorted
        BasicReport(result.mkString(s"${result.size} invokedynamic instructions found:\n", "\n", "\n"))
    }

}
