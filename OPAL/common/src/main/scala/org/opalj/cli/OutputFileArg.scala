/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.fileConverter

import java.io.File

object OutputFileArg extends PlainArg[File] {
    override val name: String = "out"
    override val description: String = "File to write output to"
}
