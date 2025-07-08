/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.io.File

import org.rogach.scallop.fileConverter

object ResultFileArg extends PlainArg[File] {
    override val name: String = "result"
    override val description: String = "File to write result to"

    def getResultFile(config: OPALCommandLineConfig, execution: Int): File = {
        var resultFile = config(ResultFileArg)
        if (execution != 1) {
            resultFile = new File(resultFile.getPath + s"_$execution")
        }
        resultFile
    }
}
