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
 * Prints out the method-level annotations of all methods. (I.e., class, field and
 * parameter annotations are not printed.)
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object MethodAnnotationsPrinter extends ProjectsAnalysisApplication {

    protected class MethodAnnotationsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects annotations on methods"
    }

    protected type ConfigType = MethodAnnotationsConfig

    protected def createConfig(args: Array[String]): MethodAnnotationsConfig = new MethodAnnotationsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodAnnotationsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val annotations =
            for {
                classFile <- project.allClassFiles.par
                method <- classFile.methods
                annotation <- method.runtimeVisibleAnnotations ++ method.runtimeInvisibleAnnotations
            } yield {
                method.toJava +
                    annotation.elementValuePairs.map { pair =>
                        "%-15s: %s".format(pair.name, pair.value.toJava)
                    }.mkString(s"\n\t@${annotation.annotationType.toJava}\n\t", "\n\t", "\n")
            }

        (project, BasicReport(annotations.mkString(s"${annotations.size} annotations found:\n", "\n", "\n")))
    }
}
