package org.opalj.support.parser

import org.opalj.Commandline_base.commandlines.OpalCommandExternalParser
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse

object DomainCommandExternalParser extends OpalCommandExternalParser {
    override def parse[T](arg: T) :  Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse] = {
        val domain = arg.asInstanceOf[String]
        if (domain == null)
            classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        else {
            Class.forName(domain).asInstanceOf[Class[Domain with RecordDefUse]]
        }
    }
}
