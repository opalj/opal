/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

object RaterCommand extends OpalPlainCommand[String] {
    override var name: String = "rater"
    override var argName: String = "rater"
    override var description: String = "class name of the rater for domain-specific actions"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
