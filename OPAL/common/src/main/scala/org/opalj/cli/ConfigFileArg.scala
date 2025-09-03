/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

object ConfigFileArg extends PlainArg[String] {
    override val name: String = "config"
    override val description: String = "A .conf file providing OPAL configuration values"
    override val argName: String = "file"
}
