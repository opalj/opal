/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringListConverter

object PackagesArg extends PlainArg[List[String]] {
    override val name: String = "packages"
    override val description: String = "List of packages to process, e.g. java/util javax"
}
