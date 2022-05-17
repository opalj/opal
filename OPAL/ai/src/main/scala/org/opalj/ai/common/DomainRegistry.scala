/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package common

import com.typesafe.config.Config

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * Registry for all domains that can be instantiated given a `Project`, and a `Method` with a
 * body.
 *
 * The registry was developed to support tools for enabling the automatic selection of a domain
 * that satisfies a given set of requirements; it also support debugging purposes that let
 * the user/developer choose between different domains. After choosing a domain,
 * an abstract interpretation can be performed.
 *
 * The compatible domains that are part of OPAL are already registered.
 *
 * ==Thread Safety==
 * The registry is thread safe.
 *
 * @author Michael Eichberg
 */
object DomainRegistry {

    case class DomainMetaInformation(
            lessPreciseDomains: Set[Class[_ <: Domain]],
            factory:            (SomeProject, Method) => Domain
    )

    type ClassRegistry = Map[Class[_ <: Domain], DomainMetaInformation]

    private[this] var descriptions: Map[String, Class[_ <: Domain]] = Map.empty
    private[this] var classRegistry: ClassRegistry = Map.empty

    /**
     * Register a new domain that can be used to perform an abstract interpretation
     * of a specific method.
     *
     * @param  domainDescription A short description of the properties of the domain;
     *         in particular w.r.t. the kind of computations the domain does.
     * @param  lessPreciseDomains The set of domains which are less precise/costly than this domain.
     *         This basically defines a partial order between the domains.
     * @param  domainClass The class of the domain.
     * @param  factory The factory method that will be used to create instances of the domain.
     */
    def register(
        domainDescription:  String,
        domainClass:        Class[_ <: Domain],
        lessPreciseDomains: Set[Class[_ <: Domain]],
        factory:            (SomeProject, Method) => Domain
    ): Unit = {
        this.synchronized {
            if (classRegistry.get(domainClass).nonEmpty)
                throw new IllegalArgumentException(s"$domainClass is already registered");

            descriptions += ((domainDescription, domainClass))
            classRegistry += ((domainClass, DomainMetaInformation(lessPreciseDomains, factory)))
        }
    }

    /** The transitive hull of all less precise domains of the given domain. */
    def allLessPreciseDomains(rootDomainClass: Class[_ <: Domain]): Set[Class[_ <: Domain]] = {
        var domains = Set.empty[Class[_ <: Domain]]
        var domainsToAnalyze = classRegistry(rootDomainClass).lessPreciseDomains
        while (domainsToAnalyze.nonEmpty) {
            val domain = domainsToAnalyze.head
            domainsToAnalyze = domainsToAnalyze.tail
            domains += domain
            classRegistry(domain).lessPreciseDomains.foreach { d =>
                if (!domains.contains(d)) domainsToAnalyze += d
            }
        }

        domains
    }

    def selectCandidates(requirements: Iterable[Class[_ <: AnyRef]]): Set[Class[_ <: Domain]] = {
        classRegistry.keys.filter { candidate =>
            requirements.forall(r => r.isAssignableFrom(candidate))
        }.toSet
    }

    /**
     * Selects a domain that satisfies all requirements and which – according to the domains' partial
     * order is the most precise one. If the most precise one is not unique multiple domains are
     * returned; if no domain satisfies the requirements an empty sequence is returned.
     *
     * @example
     *          To get a domain use:
     *          {{{
     *          selectBest(Seq(classOf[RecordDefUse],classOf[IntegerRangeValues] ))
     *          }}}
     *
     * @return The best domain satisfying the stated requirements.
     */
    def selectBest(requirements: Iterable[Class[_ <: AnyRef]]): Set[Class[_ <: Domain]] = {
        val candidateClasses = selectCandidates(requirements)
        if (candidateClasses.isEmpty)
            return Set.empty;

        val d: Class[_ <: Domain] = candidateClasses.head
        val rootSet = Set[Class[_ <: Domain]](d)
        val (best, _) = candidateClasses.tail.foldLeft((rootSet, allLessPreciseDomains(d))) { (c, n) =>
            // select the most precise domains...
            val (candidateDomains, lessPreciseThanCurrent) = c
            if (lessPreciseThanCurrent.contains(n))
                c
            else {
                val lessPreciseThanN = allLessPreciseDomains(n)
                (
                    (candidateDomains -- lessPreciseThanN) + n,
                    lessPreciseThanCurrent ++ lessPreciseThanN
                )
            }
        }
        best
    }

    def selectCheapest(requirements: Iterable[Class[_ <: AnyRef]]): Set[Class[_ <: Domain]] = {
        val candidateClasses = selectCandidates(requirements)
        if (candidateClasses.isEmpty)
            return Set.empty;

        val d: Class[_ <: Domain] = candidateClasses.head
        val rootSet = Set[Class[_ <: Domain]](d)
        candidateClasses.tail.foldLeft(rootSet) { (c, n) =>
            // select the least precise/cheapest domains...
            val lessPreciseThanN = allLessPreciseDomains(n)
            if (lessPreciseThanN.exists(c.contains)) {
                // we already have a less precise domain...
                c
            } else {
                // This one is less precise than all other, is one the other acutally
                // more precise than N
                c.filter(c => !allLessPreciseDomains(c).contains(n)) + n
            }
        }
    }

    final val configStrategySelectionKey = "org.opalj.ai.common.DomainRegistry.defaultStrategy"

    def selectConfigured(
        config:       Config,
        requirements: Iterable[Class[_ <: AnyRef]]
    ): Set[Class[_ <: Domain]] = {
        config.getString(configStrategySelectionKey) match {
            case "cheapest" => selectCheapest(requirements)
            case "best"     => selectBest(requirements)
            case s          => throw new UnsupportedOperationException(s"unknown strategy: $s")
        }
    }

    /**
     * Returns an `Iterable` to make it possible to iterate over the descriptions of
     * the domain. Useful to show the (end-users) some meaningful descriptions.
     */
    def domainDescriptions(): Iterable[String] = this.synchronized {
        for ((d, c) <- descriptions) yield s"[${c.getName}] $d"
    }

    /**
     * Returns the current view of the registry.
     */
    def registry: ClassRegistry = this.synchronized { classRegistry }

    /**
     * Creates a new instance of the domain identified by the given `domainDescription`.
     *
     * @param domainDescription The description that identifies the domain.
     * @param project The project.
     * @param method A method with a body.
     */
    // primarily introduced to facilitate the interaction with Java
    def newDomain(
        domainDescription: String,
        project:           SomeProject,
        method:            Method
    ): Domain = {
        this.synchronized {
            val domainClass: Class[_ <: Domain] = descriptions(domainDescription)
            newDomain(domainClass, project, method)
        }
    }

    /**
     * Creates a new instance of the domain identified by the given `domainClass`. To
     * create the instance the registered factory method will be used.
     *
     * @param domainClass The class object of the domain.
     * @param project The project.
     * @param method A method with a body.
     */
    def newDomain(domainClass: Class[_ <: Domain], project: SomeProject, method: Method): Domain = {
        this.synchronized { classRegistry(domainClass).factory(project, method) }
    }

    def domainMetaInformation(domainClass: Class[_ <: Domain]): DomainMetaInformation = {
        this.synchronized { classRegistry(domainClass) }
    }

    // initialize the registry with the known default domains

    // IMPROVE Add functionality to the domains to provide a description and then use that information when registering the domain factory
    register(
        "computations are done at the type level",
        classOf[domain.l0.BaseDomain[_]],
        lessPreciseDomains = Set.empty,
        (project: SomeProject, method: Method) => new domain.l0.BaseDomain(project, method)
    )

    register(
        "computations are done at the type level; cfg and def/use information is recorded",
        classOf[domain.l0.PrimitiveTACAIDomain],
        lessPreciseDomains = Set(classOf[domain.l0.BaseDomain[_]]),
        (project: SomeProject, method: Method) => new domain.l0.BaseDomainWithDefUse(project, method)
    )

    register(
        "computations are done at the type level; "+
            "cfg and def/use information is recorded; "+
            "signature refinements are used",
        classOf[fpcf.domain.PrimitiveTACAIDomainWithSignatureRefinement],
        lessPreciseDomains = Set(classOf[domain.l0.PrimitiveTACAIDomain]),
        (project: SomeProject, method: Method) => {
            new fpcf.domain.PrimitiveTACAIDomainWithSignatureRefinement(project, method)
        }
    )

    register(
        "computations related to int values are done using intervals",
        classOf[domain.l1.DefaultIntervalValuesDomain[_]],
        lessPreciseDomains = Set(classOf[domain.l0.BaseDomain[_]]),
        (project: SomeProject, method: Method) => {
            new domain.l1.DefaultIntervalValuesDomain(project, method)
        }
    )

    register(
        "computations related to int/long values are done using sets",
        classOf[domain.l1.DefaultSetValuesDomain[_]],
        lessPreciseDomains = Set(classOf[domain.l0.BaseDomain[_]]),
        (project: SomeProject, method: Method) => {
            new domain.l1.DefaultSetValuesDomain(project, method)
        }
    )

    register(
        "computations related to reference types track nullness, must alias and origin information",
        classOf[domain.l1.DefaultReferenceValuesDomain[_]],
        lessPreciseDomains = Set(classOf[domain.l0.BaseDomain[_]]),
        (project: SomeProject, method: Method) => {
            new domain.l1.DefaultReferenceValuesDomain(project, method)
        }
    )

    register(
        "computations related to reference types track nullness, must alias and origin information; records the ai-time def-use information",
        classOf[domain.l1.DefaultReferenceValuesDomainWithCFGAndDefUse[_]],
        lessPreciseDomains = Set(classOf[domain.l0.PrimitiveTACAIDomain]),
        (project: SomeProject, method: Method) => {
            new domain.l1.DefaultReferenceValuesDomainWithCFGAndDefUse(project, method)
        }
    )

    register(
        "computations related to ints use intervals; tracks nullness, must alias and origin information of reference values",
        classOf[domain.l1.DefaultDomain[_]],
        lessPreciseDomains = Set(classOf[domain.l0.BaseDomain[_]]),
        (project: SomeProject, method: Method) => {
            new domain.l1.DefaultDomain(project, method)
        }
    )

    register(
        "uses intervals for int values; tracks nullness and must alias information for reference types; records the ai-time def-use information",
        classOf[domain.l1.DefaultDomainWithCFGAndDefUse[_]],
        lessPreciseDomains = Set(classOf[domain.l0.PrimitiveTACAIDomain]),
        (project: SomeProject, method: Method) => {
            new domain.l1.DefaultDomainWithCFGAndDefUse(project, method)
        }
    )

    register(
        "uses intervals for int values; "+
            "tracks nullness and must alias information for reference types; "+
            "records the ai-time def-use information; "+
            "uses refined signature information",
        classOf[fpcf.domain.L1DefaultDomainWithCFGAndDefUseAndSignatureRefinement[_]],
        lessPreciseDomains = Set(
            classOf[domain.l1.DefaultDomainWithCFGAndDefUse[_]],
            classOf[fpcf.domain.PrimitiveTACAIDomainWithSignatureRefinement]
        ),
        (project: SomeProject, method: Method) => {
            new fpcf.domain.L1DefaultDomainWithCFGAndDefUseAndSignatureRefinement(project, method)
        }
    )

    register(
        "uses intervals for int values; tracks nullness and must alias information for reference types; records the ai-time def-use information; performs simple method invocations",
        classOf[domain.l2.DefaultPerformInvocationsDomain[_]],
        lessPreciseDomains = Set(
            classOf[domain.l1.DefaultIntervalValuesDomain[_]],
            classOf[domain.l1.DefaultReferenceValuesDomain[_]]
        ),
        (project: SomeProject, method: Method) => {
            new domain.l2.DefaultPerformInvocationsDomain(project, method)
        }
    )

    register(
        "performs simple method invocations additionally to performing int computations using intervals and; records the ai-time def-use information",
        classOf[domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[_]],
        lessPreciseDomains = Set(
            classOf[domain.l1.DefaultDomainWithCFGAndDefUse[_]],
            classOf[domain.l2.DefaultPerformInvocationsDomain[_]]
        ),
        (project: SomeProject, method: Method) => {
            new domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse(project, method)
        }
    )

    register(
        "called methods are context-sensitively analyzed (up to two levels per default)",
        classOf[domain.l2.DefaultDomain[_]],
        lessPreciseDomains = Set(classOf[domain.l2.DefaultPerformInvocationsDomain[_]]),
        (project: SomeProject, method: Method) => new domain.l2.DefaultDomain(project, method)
    )

}
