/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package parser

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.commandlinebase.OpalCommandExternalParser

/**
 * `DomainCommandExternalParser` is a parser used to resolve and load a specified domain class.
 * It maps a command-line argument to a `Class` type that implements `Domain` and `RecordDefUse`, enabling dynamic
 * selection of a domain type for AI analysis configurations.
 */

object DomainCommandExternalParser
    extends OpalCommandExternalParser[
        String,
        Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse]
    ] {
    override def parse(
        arg: String
    ): Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse] = {
        if (arg == null)
            classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        else Class.forName(arg).asInstanceOf[Class[Domain with RecordDefUse]]
    }
}
