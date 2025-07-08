/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Counts the number of native methods.
 *
 * @author Michael Eichberg
 */
object NativeMethodsCounter extends ProjectsAnalysisApplication {

    protected class NativeMethodsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Counts the number of native methods"
    }

    protected type ConfigType = NativeMethodsConfig

    protected def createConfig(args: Array[String]): NativeMethodsConfig = new NativeMethodsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: NativeMethodsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val nativeMethods = project.allClassFiles.flatMap(_.methods.filter(_.isNative).map(_.toJava))
        (project, BasicReport(nativeMethods.mkString(s"${nativeMethods.size} native methods found:\n\t", "\n\t", "\n")))
    }
}
