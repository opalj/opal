package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse

/**
 * `DomainCommandExternalParser` is a parser used to resolve and load a specified domain class.
 * It maps a command-line argument to a `Class` type that implements `Domain` and `RecordDefUse`, enabling dynamic
 * selection of a domain type for AI analysis configurations.
 */

object DomainCommandExternalParser extends OpalCommandExternalParser[Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse]] {
    override def parse[T](arg: T) :  Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse] = {
        val domain = arg.asInstanceOf[String]
        if (domain == null)
            classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        else Class.forName(domain).asInstanceOf[Class[Domain with RecordDefUse]]
    }
}
