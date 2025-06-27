/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.flagConverter

object ClosedWorldCommand extends PlainCommand[Boolean] {
    override val name: String = "cwa"
    override val argName: String = "cwa"
    override val description: String = "Use closed world assumption, i.e., no class can be extended"
    override val defaultValue: Option[Boolean] = Some(false)

    override def apply(config: Config, value: Option[Boolean]): Config = {
        if (value.get)
            config.withValue(
                "org.opalj.br.analyses.cg.ClassExtensibilityKey.analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ClassHierarchyIsNotExtensible")
            )
        else config
    }
}
