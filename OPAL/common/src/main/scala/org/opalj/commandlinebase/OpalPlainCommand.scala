/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

trait OpalPlainCommand[T] extends OpalCommand {
    var name: String
    var argName: String
    var description: String
    var defaultValue: Option[T]
    var noshort: Boolean
}
