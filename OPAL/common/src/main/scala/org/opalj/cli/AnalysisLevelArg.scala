/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

case class AnalysisLevelArg(override val description: String, val levels: (String, String)*)
    extends ParsedArg[String, String] with ChoiceArg[String] {
    override val name: String = "level"

    val withNone = true

    override val choices = {
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
