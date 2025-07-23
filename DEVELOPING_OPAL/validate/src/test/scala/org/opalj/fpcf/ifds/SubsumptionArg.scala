/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object SubsumptionArg extends PlainArg[Boolean] {
    override val name: String = "subsume"
    override val description: String = "Subsume facts (enables sharing)"
    override val defaultValue: Option[Boolean] = Some(false)
}
