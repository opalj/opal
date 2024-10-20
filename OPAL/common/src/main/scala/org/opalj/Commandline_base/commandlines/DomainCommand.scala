package org.opalj.Commandline_base.commandlines

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse

object DomainCommand extends OpalPlainCommand[String] {
    override var name: String = "domain"
    override var argName: String = "domain"
    override var description: String = "class name of the abstract interpretation domain"
    override var defaultValue: Option[String] = None
    override var noshort: Boolean = true

    def parse(domain: String) :  Class[_ >: DefaultPerformInvocationsDomainWithCFGAndDefUse[_] <: Domain with RecordDefUse] = {
        if (domain == null)
            classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        else {
            Class.forName(domain).asInstanceOf[Class[Domain with RecordDefUse]]
        }
    }
}
