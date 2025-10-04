package org.opalj
package cli

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

object KeepPropertyKeysArg extends ParsedArg[List[String], List[String]] {
    override val name: String = "keepPropertyKeys"
    override val description: String = "List of Properties to keep at the end of the analysis"
    override val defaultValue: Option[List[String]] = None

    override def apply(config: Config, value: Option[List[String]]): Config = {
        config.withValue(
            "org.opalj.fpcf.AnalysisScenario.KeepPropertyKeys",
            ConfigValueFactory.fromAnyRef(value.getOrElse(""))
        )
    }

    override def parse(arg: List[String]): List[String] = {
        arg.flatMap(_.split(","))
    }
}
