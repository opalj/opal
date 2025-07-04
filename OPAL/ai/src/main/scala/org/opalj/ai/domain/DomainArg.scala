/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.ProjectBasedArg
import org.opalj.cli.ParsedArg
import org.rogach.scallop.stringConverter

object DomainArg extends ParsedArg[String, Class[_ <: Domain]] with ProjectBasedArg[String, Class[_ <: Domain]] {
    override val name: String = "domain"
    override val argName: String = "fqn"
    override val description: String = "Fully-qualified class name of the abstract interpretation domain to use"
    override val defaultValue: Option[String] = Some(classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]].getName)

    override def parse(arg: String): Class[_ <: Domain] = {
        Class.forName(arg).asInstanceOf[Class[Domain]]
    }

    override def apply(project: SomeProject, value: Option[Class[_ <: Domain]]): Unit = {
        value.foreach { domain =>
            project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
                case None               => Set(domain)
                case Some(requirements) => requirements + domain
            }
        }
    }
}
