/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Shows the local variable type tables of given class files.
 *
 * @author Daniel Klauer
 */
object LocalVariableTypeTables extends ProjectsAnalysisApplication {

    protected class LocalVariableTablesConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Prints the local variable type tables"
    }

    protected type ConfigType = LocalVariableTablesConfig

    protected def createConfig(args: Array[String]): LocalVariableTablesConfig = new LocalVariableTablesConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: LocalVariableTablesConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val messages = new ConcurrentLinkedQueue[String]()
        project.parForeachMethodWithBody() { mi =>
            val m = mi.method
            val lvtt = m.body.get.localVariableTypeTable
            if (lvtt.nonEmpty)
                messages.add(
                    Console.BOLD + Console.BLUE + m.toJava + Console.RESET + " " +
                        lvtt.mkString("LocalVariableTypeTable: ", ",", "")
                )
        }

        (project, BasicReport(messages.asScala.mkString("\n", "\n\n", "\n")))
    }
}
