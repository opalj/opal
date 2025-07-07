/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.io.File

import org.rogach.scallop.fileConverter

object OutputFileArg extends PlainArg[File] {
    override val name: String = "out"
    override val description: String = "File to write output to"

    def getOutputFile(config: OPALCommandLineConfig, execution: Int): File = {
        var outputFile = config(OutputFileArg)
        if (execution != 1) {
            outputFile = new File(outputFile.getPath + s"_$execution")
        }
        outputFile
    }
}
