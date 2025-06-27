/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.io.File

import org.rogach.scallop.stringConverter

object EvalDirCommand extends ParsedCommand[String, File] {
    override val name: String = "evalDir"
    override val argName: String = "path"
    override val description: String = "Path to evaluation directory"

    def parse(evalDir: String): File = {
        val evaluationDir = new File(evalDir)
        if (!evaluationDir.exists()) evaluationDir.mkdir

        evaluationDir
    }
}
