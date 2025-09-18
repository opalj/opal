package org.opalj.cli

import com.typesafe.config.{Config, ConfigValueFactory}

object ClearPropertyKeysArg extends ParsedArg[String, String] {
    override val name: String = "clearPropertyKeys"
    override val description: String = "List of Properties to keep at the end of the analysis"
    override val defaultValue: Option[String] = None
    override def apply(config: Config, value: Option[String]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.ClearPropertyKeys",
            ConfigValueFactory.fromAnyRef(value.getOrElse(""))
        )
    }

    override def parse(arg: String): String = arg.trim
}