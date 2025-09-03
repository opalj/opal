/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.cli

import org.rogach.scallop.flagConverter

import org.opalj.cli.PlainArg

object TraceArg extends PlainArg[Boolean] {
    override val name: String = "trace"
    override val description: String = "Trace the abstract interpretation"
    override val defaultValue: Option[Boolean] = Some(false)
}
