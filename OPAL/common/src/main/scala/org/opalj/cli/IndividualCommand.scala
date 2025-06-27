/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object IndividualCommand extends PlainCommand[Boolean] {
    override val name: String = "individual"
    override val description: String = "Report results individually instead of in aggregated form"
    override val defaultValue: Option[Boolean] = Some(false)
}
