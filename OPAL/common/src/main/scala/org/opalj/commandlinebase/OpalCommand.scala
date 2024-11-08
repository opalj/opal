/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package commandlinebase

trait OpalCommand {
    def parse[T](arg: T): Any
}
