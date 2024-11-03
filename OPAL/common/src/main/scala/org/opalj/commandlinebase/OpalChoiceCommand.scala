/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.commandlinebase

trait OpalChoiceCommand extends OpalCommand {
    var name: String
    var argName: String
    var description: String
    var defaultValue: Option[String]
    var noshort: Boolean
    var choices: Seq[String]
}
