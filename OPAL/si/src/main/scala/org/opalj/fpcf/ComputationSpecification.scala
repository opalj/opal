/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

case class SpecificationViolation(message: String) extends Exception(message)

/**
 * Specification of the properties and the life-cycle methods of a fix-point computation
 * (FPC) that are relevant when computing the correct scheduling order and actually executing
 * the analysis.
 *
 * @author Michael Eichberg
 */
trait ComputationSpecification {

    type InitializationData

    /**
     * Returns a short descriptive name of the analysis which is described by this specification.
     *
     * The default name is the name of `this` class.
     *
     * '''This method should be overridden.'''
     */
    def name: String = {
        val nameCandidate = this.getClass.getSimpleName
        if (nameCandidate.endsWith("$"))
            nameCandidate.substring(0, nameCandidate.length() - 1)
        else
            nameCandidate
    }

    /**
     * Returns the kinds of properties which are queried by this analysis.
     *
     * @note   This set consists only of property kinds which are directly used by the analysis.
     *
     * @note   Self usages don't have to be documented since the analysis will derive this
     *         property during the computation.
     */
    def uses: Set[PropertyKind]

    /**
     * Returns the set of property kinds derived by the underlying analysis.
     */
    def derives: Set[PropertyKind]

    require(derives.nonEmpty, "the computation does not derive any information")

    /**
     * Has to be true if a computation is performed lazily. This is used to check that we
     * never schedule multiple analyses which compute the same kind of property.
     */
    def isLazy: Boolean

    /**
     * Called before any analysis is called/scheduled that will be executed in the same phase
     * to enable further initialization of the computations that will eventually be executed.
     * For example to initialize global configuration information.
     *
     * If an [[AnalysisScenario]] is used to compute a schedule and execute it later on, `init`
     * will be called before any analysis – independent – of the batch in which it will run,
     * is called.
     *
     * A computation specification MUST NOT call any methods of the property store that
     * may trigger or schedule computations; i.e., it must – in particular – not call
     * the methods `apply`, `schedule*`, `register*` or `waitOnPhaseCompletion`.
     *
     * @note This method is intended to be overwritten by sub classes. The default implementation
     *       does nothing.
     */
    def init(ps: PropertyStore): InitializationData

    /**
     * Called directly before the analysis is scheduled. I.e., after phase setup.
     */
    def beforeSchedule(ps: PropertyStore): Unit

    /**
     * Called after phase completion.
     */
    def afterPhaseCompletion(ps: PropertyStore): Unit

    /**
     * Called by the scheduler to start execution of this analysis.
     *
     * The analysis may very well be a lazy computation.
     */
    def schedule(ps: PropertyStore, i: InitializationData): Unit

    override def toString: String = {
        val uses =
            this.uses.iterator.map(u ⇒ PropertyKey.name(u)).mkString("uses={", ", ", "}")
        val derives =
            this.derives.iterator.map(d ⇒ PropertyKey.name(d)).mkString("derives={", ", ", "}")
        s"FPC(name=$name,$uses,$derives)"
    }

}
