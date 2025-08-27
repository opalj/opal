/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.stringConverter

object GranularityArg extends ParsedArg[String, Boolean] with ChoiceArg[Boolean] {
    override val name: String = "granularity"
    override val description: String = "Whether to use perform a coarse- or fine-grained analysis"
    override val choices: Seq[String] = Seq("coarse", "fine")

    override def parse(arg: String): Boolean = arg match {
        case "coarse" => false
        case "fine"   => true
    }
}
