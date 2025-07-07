/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.util

import scala.language.postfixOps

import org.opalj.ai.domain.DomainArg
import org.opalj.cli.OPALCommandLineConfig

import org.rogach.scallop.ScallopConf

trait AIBasedCommandLineConfig extends OPALCommandLineConfig { self: ScallopConf =>

    val aiArgGroup = group("Arguments related to abstract interpretation:")

    val aiArgs = Seq(
        DomainArg !
    )

    args(aiArgs *)

    aiArgs.foreach { arg => argGroups += arg -> aiArgGroup }
}
