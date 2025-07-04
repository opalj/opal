/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.fileConverter

import java.io.File

object ResultFileArg extends PlainArg[File] {
    override val name: String = "result"
    override val description: String = "File to write result to"
}
