/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package purity

import org.rogach.scallop.stringConverter

import org.opalj.cli.ParsedArg
import org.opalj.log.GlobalLogContext
import org.opalj.util.getObjectReflectively

object RaterArg extends ParsedArg[String, DomainSpecificRater] {
    override val name: String = "rater"
    override val argName: String = "fqn"
    override val description: String = "Fully-qualified class name of the rater for domain-specific actions"
    override val defaultValue: Option[String] = Some(SystemOutLoggingAllExceptionRater.getClass.getName)

    override def parse(arg: String): DomainSpecificRater = {
        getObjectReflectively(this, "analysis configuration", arg)(GlobalLogContext).get
    }
}
