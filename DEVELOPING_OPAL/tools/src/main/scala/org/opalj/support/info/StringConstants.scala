/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.StringConstantsInformationKey
import org.opalj.br.analyses.BasicReport

/**
 * Prints out all string constants found in the bytecode.
 *
 * @author Michael Eichberg
 */
object StringConstants extends ProjectAnalysisApplication {

    override def title: String = "String Constants"

    override def description: String = {
        "Collects all constant strings (based on LDC instructions) found in the specified code."
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val data = project.get(StringConstantsInformationKey)
        val mappedData: Iterable[String] = data.map { kv =>
            val (string, locations) = kv
            val escapedString = string.
                replace("\u001b", "\\u001b").
                replace("\n", "\\n").
                replace("\t", "\\t").
                replace("\"", "\\\"")
            locations.map { pcInMethod =>
                val pc = pcInMethod.pc
                val method = pcInMethod.method
                method.toJava(s"pc=$pc")
            }.mkString("\""+escapedString+"\":\n\t - ", "\n\t - ", "\n")
        }

        BasicReport(mappedData.mkString(s"\nFound ${data.size} strings:\n", "\n", "\n"))
    }
}
