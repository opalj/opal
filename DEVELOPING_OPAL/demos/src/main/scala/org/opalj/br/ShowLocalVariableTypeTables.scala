/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication

/**
 * Shows the local variable type tables of given class files.
 *
 * @author Daniel Klauer
 */
object ShowLocalVariableTypeTables extends ProjectAnalysisApplication {

    override def description: String = "Prints out the local variable type tables."

    def doAnalyze(
        project:       Project[URL],
        params:        Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val messages = new ConcurrentLinkedQueue[String]()
        project.parForeachMethodWithBody(isInterrupted) { mi =>
            val m = mi.method
            val lvtt = m.body.get.localVariableTypeTable
            if (lvtt.nonEmpty)
                messages.add(
                    Console.BOLD + Console.BLUE + m.toJava + Console.RESET+" "+
                        lvtt.mkString("LocalVariableTypeTable: ", ",", "")
                )
        }

        BasicReport(messages.asScala.mkString("\n", "\n\n", "\n"))
    }
}
