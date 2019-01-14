/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

case class SpecificationViolation(message: String) extends Exception(message)

/**
 * Specification of the properties and the life-cycle methods of a fixpoint computation
 * (FPC) which are required when computing the correct scheduling order and actually executing
 * the fixpoint computation.
 *
 * @note The [[PropertyStore]] can be used without using [[ComputationSpecification]]s and
 *       [[AnalysisScenario]]s; both latter classes just provide convenience functionality
 *       that ensures that common issues are identified early on/are avoided.
 *
 * @author Michael Eichberg
 */
trait ComputationSpecification[A] {

    //
    // PROPERTIES OF COMPUTATION SPECIFICATIONS
    //

    /**
     * Identifies this computation specification; typically the name of the class
     * which implements the underlying analysis.
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
     * @note   Self usages should also be documented.
     *
     * @note   This method is called after
     *         [[org.opalj.fpcf.ComputationSpecification#init(ps:org\.opalj\.fpcf\.PropertyStore)*]]
     *         was called for all analyses belonging to an analysis scenario.
     *         (E.g., it can be used to collect the set of used property bounds based on the
     *         configuration choices made in other analyses.)
     */
    def uses(ps: PropertyStore): Set[PropertyBounds]

    /**
     * Returns the kind of the property that is lazily (on-demand) derived.
     */
    def derivesLazily: Option[PropertyBounds]

    /**
     * Returns the set of property kinds eagerly derived by the underlying analysis.
     */
    def derivesEagerly: Set[PropertyBounds]

    def derivesCollaboratively: Set[PropertyBounds]

    def derives: Iterator[PropertyBounds] = {
        derivesEagerly.iterator ++ derivesCollaboratively.iterator ++ derivesLazily.iterator
    }

    require(
        (derivesCollaboratively intersect derivesEagerly).isEmpty,
        "a property either has to be derived eagerly or collaboratively, but not both: "+
            (derivesCollaboratively intersect derivesEagerly).mkString(", ")
    )

    require(
        derivesLazily.isDefined || derivesEagerly.nonEmpty || derivesCollaboratively.nonEmpty,
        "the computation does not derive any information"
    )

    require(
        derivesLazily.isEmpty || (derivesEagerly.isEmpty && derivesCollaboratively.isEmpty),
        "a lazy analysis cannot also derive information eagerly and/or collaboratively "
    )

    /**
     * Specifies the kind of the computation that is performed. The kind restricts in which
     * way the analysis is allowed to interact with the property store/other analyses.
     */
    def computationType: ComputationType

    def toString(ps: PropertyStore): String = {
        val uses = this.uses(ps).iterator.map(_.toSpecification).mkString("uses={", ", ", "}")

        val derivesLazily =
            this.derivesLazily.iterator.
                map(_.toSpecification).
                mkString("derivesLazily={", ", ", "}")
        val derivesEagerly =
            this.derivesEagerly.iterator.
                map(_.toSpecification).
                mkString("derivesEagerly={", ", ", "}")
        val derivesCollaboratively =
            this.derivesCollaboratively.iterator.
                map(_.toSpecification).
                mkString("derivesCollaboratively={", ", ", "}")

        s"ComputationSpecification(name=$name,type=$computationType,"+
            s"$uses,$derivesLazily,$derivesEagerly,$derivesCollaboratively)"

    }

    override def toString: String = {
        s"ComputationSpecification(name=$name,type=$computationType)"
    }

    //
    // LIFECYCLE RELATED METHODS
    //

    /**
     * The type of the data used by the analysis at initialization time.
     * For analyses without special initialization requirements this type is `Null`.
     */
    type InitializationData

    /**
     * Called directly after the analysis is registered with an analysis scheduler; in particular
     * before any analysis belonging to the same analysis scenario is scheduled –
     * independent of the batch in which it will run.
     *
     * This enables further initialization of the computations that will eventually be executed.
     * For example to initialize global configuration information.
     *
     * A computation specification does not have to call any methods of the property store that
     * may trigger or schedule computations; i.e., it must – in particular – not call
     * the methods `apply`, `schedule*`, `register*` or `waitOnPhaseCompletion`.
     *
     * @return The initialization data that is later on passed to schedule.
     */
    def init(ps: PropertyStore): InitializationData

    /**
     * Called directly before the analyses belonging to a phase are effectively scheduled. I.e.,
     * after phase setup, but potentially after other analyses' `beforeSchedule` method is called.
     */
    def beforeSchedule(ps: PropertyStore): Unit

    /**
     * Called by the scheduler to let the analysis register itself or to start execution.
     */
    def schedule(ps: PropertyStore, i: InitializationData): A

    /**
     * Called back after all analyses of a specific phase have been
     *        schedule (i.e., before calling waitOnPhaseCompletion).
     */
    def afterPhaseScheduling(ps: PropertyStore, analysis: A): Unit

    /**
     * Called after phase completion.
     */
    def afterPhaseCompletion(ps: PropertyStore, analysis: A): Unit

}

trait SimpleComputationSpecification[A] extends ComputationSpecification[A] {

    final override type InitializationData = Null
    final override def init(ps: PropertyStore): Null = null
    final override def beforeSchedule(ps: PropertyStore): Unit = {}
    final override def afterPhaseScheduling(ps: PropertyStore, analysis: A): Unit = {}
    final override def afterPhaseCompletion(ps: PropertyStore, analysis: A): Unit = {}

}
