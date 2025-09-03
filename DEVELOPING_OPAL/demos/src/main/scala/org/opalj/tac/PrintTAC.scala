/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File
import java.net.URL

import org.opalj.ai.cli.AIBasedCommandLineConfig
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Prints the 3-address code for all methods of all classes found in the given project.
 *
 * @author Michael Eichberg
 */
object PrintTAC extends ProjectsAnalysisApplication {

    protected class TACConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "Prints the 3-address code for all methods of all classes"
    }

    protected type ConfigType = TACConfig

    protected def createConfig(args: Array[String]): TACConfig = new TACConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: TACConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {

        val (project, _) = analysisConfig.setupProject(cp)

        val result = new StringBuilder()

        val tacProvider = project.get(LazyDetachedTACAIKey) // TAC = Three-address code...
        for {
            cf <- project.allProjectClassFiles
            m <- cf.methods
            if m.body.isDefined
        } {
            val tac = tacProvider(m)
            result.append(m.toJava(ToTxt(tac).mkString("\n", "\n", "\n")) + "\n\n\n")
        }

        (project, BasicReport(result.toString()))
    }
}
