package org.opalj.Commandline_base.commandlines

object PackagesCommand extends OpalPlainCommand[String] {
    override var name: String = "packages"
    override var argName: String = "packages"
    override var description: String = "colon separated list of packages, e.g. java/util:javax"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    def parse(packages: String) :  Option[Array[String]] = {
        Some(packages.split(':'))
    }
}
