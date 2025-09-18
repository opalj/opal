package org.opalj.cli

import com.typesafe.config.{Config, ConfigValueFactory}

object ClearPropertyKeysArg extends ParsedArg[String, String] {
    override val name: String = "keepPropertyKeys"
    override val description: String = "List of Properties to keep at the end of the analysis"
    override val defaultValue: Option[String] = Some(String)

    override def apply(config: Config, value: Option[String]): Config = {
        val keys = value.getOrElse(Nil)
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.ClearPropertyKeys",
            ConfigValueFactory.fromAnyRef(keys)
        )
    }

    override def parse(arg: String): String = arg.trim
}