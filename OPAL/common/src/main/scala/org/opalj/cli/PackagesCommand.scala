/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

object PackagesCommand extends ParsedCommand[String, Array[String]] {
    override val name: String = "packages"
    override val description: String = "Colon separated list of packages to process, e.g. java/util:javax"

    def parse(packages: String): Array[String] = {
        packages.split(':')
    }
}
