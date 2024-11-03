/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.commandlinebase

object PackagesCommand extends OpalPlainCommand[String] {
    override var name: String = "packages"
    override var argName: String = "packages"
    override var description: String = "colon separated list of packages, e.g. java/util:javax"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    def parse[T](arg: T): Option[Array[String]] = {
        val packages = arg.asInstanceOf[String]
        Some(packages.split(':'))
    }
}
