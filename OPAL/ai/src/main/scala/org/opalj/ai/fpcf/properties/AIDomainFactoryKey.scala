/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai
package fpcf
package properties

import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.common.DomainRegistry

/**
 * Encapsulates the information which domain will be used to perform the abstract interpretations
 * for the specified project. This typically initialized by the [[AIDomainFactoryKey$]].
 */
class ProjectSpecificAIExecutor(
        val project:       SomeProject,
        val domainClass:   Class[_ <: Domain],
        val domainFactory: (SomeProject, Method) => Domain
) extends (Method => AIResult) {

    def apply(m: Method): AIResult = { BaseAI(m, domainFactory(project, m)) }
}

/**
 * Key to get the factory (actually this is a meta-factory) to create the domains that are
 * used to perform abstract interpretations.
 * The domain that is going to be used is determined by getting the set of (partial)domains
 * that are required and then computing the cheapest domain;
 * see [[org.opalj.ai.common.DomainRegistry]] for further information.
 * Hence, the `AIResult`'s domain is guaranteed to implement all required (partial) domains.
 *
 * This key's project specific initialization data are `java.lang.Class` objects which
 * have to be implemented by the finally chosen domain.
 *
 * @author Michael Eichberg
 */
object AIDomainFactoryKey
    extends ProjectInformationKey[ProjectSpecificAIExecutor, Set[Class[_ <: AnyRef]]] {

    /**
     * This key has no special prerequisites.
     *
     * @note The configuration is done using '''ProjectInformationKeyInitializationData'''.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Returns an object which performs and caches the result of the abstract interpretation of a
     * method when required.
     *
     * All methods belonging to a project are analyzed using the same `domainFactory`. Hence,
     * the `domainFactory` needs to be set before compute is called/this key is passed to a
     * specific project. If multiple projects are instead concurrently, external synchronization
     * is necessary (e.g., on the ProjectInformationKey) to ensure that each project is
     * instantiated using the desired domain.
     */
    override def compute(project: SomeProject): ProjectSpecificAIExecutor = {
        compute(project, DomainRegistry.selectConfigured(project.config, _))
    }

    def compute(
        project:         SomeProject,
        domainFactories: Iterable[Class[_ <: AnyRef]] => Set[Class[_ <: Domain]]
    ): ProjectSpecificAIExecutor = {
        implicit val logContext: LogContext = project.logContext

        val domainFactoryRequirements = project.
            getProjectInformationKeyInitializationData(this).
            getOrElse(Set.empty)

        val theDomainFactories = domainFactories(domainFactoryRequirements)

        if (theDomainFactories.isEmpty) {
            val message = domainFactoryRequirements.mkString(
                "no abstract domain that satisfies the requirements: {", ", ", "} exists."
            )
            throw new IllegalArgumentException(message)
        }
        if (theDomainFactories.size > 1) {
            OPALLogger.info(
                "analysis configuration",
                s"multiple domains ${theDomainFactories.mkString(", ")} "+
                    s"satisfy the requirements ${domainFactoryRequirements.mkString(", ")} "
            )
        }

        val domainClass = theDomainFactories.head
        OPALLogger.info(
            "analysis configuration",
            s"the domain $domainClass will be used for performing abstract interpretations"
        )

        val domainFactory = DomainRegistry.domainMetaInformation(domainClass).factory
        new ProjectSpecificAIExecutor(project, domainClass, domainFactory)
    }
}
