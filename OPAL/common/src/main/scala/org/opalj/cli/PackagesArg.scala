/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter
import org.rogach.scallop.stringListConverter

object PackagesArg extends PlainArg[List[String]] {
    override val name: String = "package"
    override val description: String = "Packages to process (including subpackages), e.g. java/util javax"
}

object MainPackageArg extends PlainArg[String] {
    override val name: String = "mainPackage"
    override val description: String = "Main package to process"
}
