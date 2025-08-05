/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import java.util.Calendar

object ConfigurationNameArg extends PlainArg[String] {
    override val name: String = "configName"
    override val argName: String = "name"
    override val description: String = "Name of the configuration run to be printed to the results file"
    override val defaultValue: Option[String] = Some(s"RUN-${Calendar.getInstance().getTime.toString}")
}
