/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.io.File

import org.rogach.scallop.fileConverter

object OutputFileArg extends OutputFileArgLike {
    override val name: String = "out"
    override val description: String = "File to write output to"
}

object ResultFileArg extends OutputFileArgLike {
    override val name: String = "result"
    override val description: String = "File to write result to"
}

trait OutputFileArgLike extends PlainArg[File] {
    def getFile(config: OPALCommandLineConfig, execution: Int): File = {
        var file = config(this)
        if (execution != 0) {
            val dir = file.getParentFile
            val name = file.getName
            val (baseName, extension) = name.splitAt(name.indexOf('.'))
            file = new File(dir, s"${baseName}_$execution$extension")
        }
        file
    }
}
