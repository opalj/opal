/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.opalj.cli.PlainArg
import org.opalj.fpcf.seq.PKESequentialPropertyStore

import org.rogach.scallop.stringConverter

object SchedulingStrategyArg extends PlainArg[String] {
    override val name: String = "schedulingStrategy"
    override val argName: String = "strategy"
    override val description: String = "Scheduling strategy to be used with the parallel PropertyStore"

    override def apply(config: Config, value: Option[String]): Config = {
        if (value.isDefined)
            config.withValue(
                PKESequentialPropertyStore.TasksManagerKey,
                ConfigValueFactory.fromAnyRef(value.get)
            )
        else
            config
    }
}
