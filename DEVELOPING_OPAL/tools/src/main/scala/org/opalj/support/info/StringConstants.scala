/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.StringConstantsInformationKey
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Prints out all string constants found in the bytecode.
 *
 * @author Michael Eichberg
 */
object StringConstants extends ProjectsAnalysisApplication {

    protected class StringConstantsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects all constant strings (based on LDC instructions) found in the specified code"
    }

    protected type ConfigType = StringConstantsConfig

    protected def createConfig(args: Array[String]): StringConstantsConfig = new StringConstantsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: StringConstantsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val data = project.get(StringConstantsInformationKey)
        val mappedData: Iterable[String] = data.map { kv =>
            val (string, locations) = kv
            val escapedString =
                string.replace("\u001b", "\\u001b").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"")
            locations.map { pcInMethod =>
                val pc = pcInMethod.pc
                val method = pcInMethod.method
                method.toJava(s"pc=$pc")
            }.mkString("\"" + escapedString + "\":\n\t - ", "\n\t - ", "\n")
        }

        (project, BasicReport(mappedData.mkString(s"\nFound ${data.size} strings:\n", "\n", "\n")))
    }
}
