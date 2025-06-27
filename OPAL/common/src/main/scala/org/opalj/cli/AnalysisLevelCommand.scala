/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

case class AnalysisLevelCommand(override val description: String, val levels: (String, String)*)
    extends ParsedCommand[String, String] with ChoiceCommand[String] {
    override val name: String = "level"
    override val argName: String = "level"

    val withNone = true

    override def choices = {
        if (withNone)
            Seq("none") :++ levels.map(_._1)
        else levels.map(_._1)
    }

    override def parse(arg: String): String = {
        if (arg == "none") ""
        else levels.toMap.getOrElse(
            arg,
            throw new IllegalArgumentException(s"Unknown analysis level for argument $name: $arg")
        )
    }
}
