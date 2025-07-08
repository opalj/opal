/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.cli.cg

import scala.jdk.CollectionConverters._

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.rogach.scallop.stringConverter

import org.opalj.br.fpcf.analyses.pointsto.TamiFlexKey
import org.opalj.cli.PlainArg

object TamiFlexArg extends PlainArg[String] {

    override val name: String = "tamiFlex"
    override val argName: String = "path"
    override val description: String = "Path to TamiFlex output file for reflection resolution"

    override def apply(config: Config, value: Option[String]): Config = {
        if (value.isDefined) {
            config.withValue(
                TamiFlexKey.configKey,
                ConfigValueFactory.fromAnyRef(value.get)
            )
        } else config
    }
}
