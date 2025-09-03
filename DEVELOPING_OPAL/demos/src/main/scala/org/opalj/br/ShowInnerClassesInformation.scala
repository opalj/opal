/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Shows the inner classes attributes of given class files.
 *
 * @author Daniel Klauer
 * @author Michael Eichberg
 */
object ShowInnerClassesInformation extends ProjectsAnalysisApplication {

    protected class InnerClassesConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Prints out the inner classes tables"
    }

    protected type ConfigType = InnerClassesConfig

    protected def createConfig(args: Array[String]): InnerClassesConfig = new InnerClassesConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: InnerClassesConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val messages =
            for {
                classFile <- project.allClassFiles.par
                if classFile.innerClasses.isDefined
            } yield {
                val header =
                    classFile.fqn + "(ver:" + classFile.majorVersion + ")" + ":\n\t" + (
                        classFile.enclosingMethod.map(_.toString).getOrElse("<no enclosing method defined>")
                    ) + "\n\t"
                classFile.innerClasses.get.mkString(header, "\n\t", "\n")
            }

        (project, BasicReport(messages.mkString("\n")))
    }

}
