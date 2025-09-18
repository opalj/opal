package org.opalj.cli

import com.typesafe.config.{Config, ConfigValueFactory}

object DisableCleanupArg extends PlainArg[Boolean] {
    override val name: String = "disableCleanup"
    override def description: String = "Disable cleanup of the PropertyStore inbetween phases"
    override val defaultValue: Option[Boolean] = Some(false)
    override def apply(config: Config, value: Option[Boolean]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.DisableCleanup",
            ConfigValueFactory.fromAnyRef(!value.get)
        )
    }
}
