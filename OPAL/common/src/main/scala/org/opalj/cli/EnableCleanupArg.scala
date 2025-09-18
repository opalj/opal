package org.opalj.cli

import com.typesafe.config.{Config, ConfigValueFactory}

object EnableCleanupArg extends PlainArg[Boolean] {
    override val name: String = "enableCleanup"
    override def description: String = "Enable cleanup of the PropertyStore inbetween phases"
    override val defaultValue: Option[Boolean] = Some(true)
    override def apply(config: Config, value: Option[Boolean]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.EnableCleanup",
            ConfigValueFactory.fromAnyRef(!value.get)
        )
    }
}
