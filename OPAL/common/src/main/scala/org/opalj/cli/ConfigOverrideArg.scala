/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.rogach.scallop.stringConverter

object ConfigOverrideArg extends PropertyArg[String] {
    override val char: Char = 'C'
    override val description: String =
        "Individual configuration values to be set (lists and objects supported: escape , and = with \\)"

    override def apply(config: Config, value: Option[Map[String, String]]): Config = {

        if (value.isDefined) {
            var newConfig = config
            for { (configKey, configValue) <- value.get } {
                val parsedValue = ConfigFactory.parseString(configKey + "=" + configValue).getValue(configKey)
                newConfig = newConfig.withValue(configKey, parsedValue)
            }
            newConfig
        } else config
    }
}
