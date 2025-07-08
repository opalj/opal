/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.util

import scala.language.postfixOps

import org.rogach.scallop.ScallopConf

import org.opalj.ai.domain.DomainArg
import org.opalj.cli.OPALCommandLineConfig

trait AIBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>

    private val aiArgGroup = group("Arguments related to abstract interpretation:")

    private val aiArgs = Seq(
        DomainArg !
    )

    args(aiArgs: _*)

    aiArgs.foreach { arg => argGroups += arg -> aiArgGroup }
}
