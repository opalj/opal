/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object NaiveTACArg extends PlainArg[Boolean] {
    override val name: String = "naive"
    override val description: String = "Use naive TAC implementation"
    override val defaultValue: Option[Boolean] = Some(false)
}
