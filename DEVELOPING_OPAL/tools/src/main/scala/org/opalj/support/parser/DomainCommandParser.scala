package org.opalj.support.parser

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse

object DomainCommandParser {
    def parse(domain: String) :  Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse] = {
        if (domain == null)
            classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        else {
            Class.forName(domain).asInstanceOf[Class[Domain with RecordDefUse]]
        }
    }
}
