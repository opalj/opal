/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Counts the number of methods that override/implement an instance method.
 *
 * @author Michael Eichberg
 */
object OverridingMethodsCount extends ProjectsAnalysisApplication {

    protected class OverridingMethodsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Counts the number of methods that override a method"
    }

    protected type ConfigType = OverridingMethodsConfig

    protected def createConfig(args: Array[String]): OverridingMethodsConfig = new OverridingMethodsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: OverridingMethodsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val overridingMethodsInfo =
            project.overridingMethods.view
                .map(ms => (ms._1, ms._2 - ms._1))
                .filter(_._2.nonEmpty).map { ms =>
                    val (method, allOverridingMethods) = ms
                    val overridingMethods = allOverridingMethods.map(m => m.classFile.fqn)
                    val count = overridingMethods.size
                    method.toJava(
                        overridingMethods.mkString(s"\n\thas $count overridde(s):\n\t\t", "\n\t\t", "\n")
                    )
                }

        (project, BasicReport("Overriding\n" + overridingMethodsInfo.mkString("\n")))
    }
}
