/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._

import com.typesafe.config.Config

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.reader.InvokedynamicRewriting.{defaultConfig => invokedynamicRewritingConfig}

/**
 * Prints out the immediately available information about invokedynamic instructions.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object Invokedynamics extends ProjectsAnalysisApplication {

    protected class InvokedynamicsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects information about invokedynamic instructions"

        override def setupConfig(isLibrary: Boolean): Config = {
            val config = super.setupConfig(isLibrary)
            // We have to adapt the configuration to ensure that invokedynamic instructions are never rewritten!
            invokedynamicRewritingConfig(rewrite = false, logRewrites = true, config)
        }
    }

    protected type ConfigType = InvokedynamicsConfig

    protected def createConfig(args: Array[String]): InvokedynamicsConfig = new InvokedynamicsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ConfigType,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val invokedynamics = new ConcurrentLinkedQueue[String]()
        project.parForeachMethodWithBody() { mi =>
            val method = mi.method
            val classFile = method.classFile
            val body = method.body.get
            invokedynamics.addAll(
                body.collectWithIndex {
                    case PCAndInstruction(pc, INVOKEDYNAMIC(bootstrap, name, descriptor)) =>
                        classFile.thisType.toJava + " {\n  " + method.signatureToJava() + "{ " + pc + ": \n" +
                            s"    ${bootstrap.toJava}\n" +
                            bootstrap.arguments.mkString("    Arguments: {", ",", "}\n") +
                            s"    Calling:   ${descriptor.toJava(name)}\n" +
                            "} }\n"
                }.asJava
            )
        }
        val result = invokedynamics.asScala.toSeq.sorted
        (project, BasicReport(result.mkString(s"${result.size} invokedynamic instructions found:\n", "\n", "\n")))
    }

}
