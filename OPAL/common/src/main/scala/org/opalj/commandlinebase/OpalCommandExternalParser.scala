/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.commandlinebase

trait OpalCommandExternalParser[T, A] {
    def parse(arg: T): Any
}
