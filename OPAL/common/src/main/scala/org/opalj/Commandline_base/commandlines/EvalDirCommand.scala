package org.opalj.Commandline_base.commandlines

import java.io.File

object EvalDirCommand extends OpalPlainCommand[String] {
    override var name: String = "evaluation directory"
    override var argName: String = "evalDir"
    override var description: String = "path to evaluation directory"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    def parse(evalDir: String) : Some[File] = {
        val evaluationDir = Some(new File(evalDir))
        if (evaluationDir.isDefined && !evaluationDir.get.exists()) evaluationDir.get.mkdir

        evaluationDir
    }
}
