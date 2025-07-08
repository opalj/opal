/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.io.File

import com.typesafe.config.Config

import org.rogach.scallop.fileConverter

object OutputDirArg extends PlainArg[File] {
    override val name: String = "evalDir"
    override val argName: String = "path"
    override val description: String = "Directory to write output files to"

    override def apply(config: Config, value: Option[File]): Config = {
        value.foreach { evaluationDir => if (!evaluationDir.exists()) evaluationDir.mkdirs() }
        config
    }
}
