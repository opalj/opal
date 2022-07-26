/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info

/**
 * The fixpoint computations framework (`fpcf`) is a general framework to perform fixpoint
 * computations of properties ordered by a lattice. The framework in particular supports the
 * development of static analyses.
 *
 * In this case, the fixpoint computations/static analyses are generally operating on the
 * code and need to be executed until the computations have reached their (implicit) fixpoint.
 * The fixpoint framework explicitly supports resolving cyclic dependencies/computations.
 * A prime use case of the fixpoint framework are all those analyses that may interact with
 * the results of other analyses.
 *
 * For example, an analysis that analyzes all field write accesses to determine if we can
 * refine a field's type (for the purpose of the analysis) can (reuse) the information
 * about the return types of methods, which however may depend on the refined field types.
 *
 * The framework is generic enough to facilitate the implementation of anytime algorithms.
 *
 * @note ''This framework assumes that all data-structures (e.g., dependee lists and properties)
 *       that are passed to the framework are effectively immutable!''
 *       (Effectively immutable means that a data structure is never updated after it was
 *       passed to the framework.)
 *
 * @note The dependency relation is as follows:
 *      “A depends on B”
 *          `===`
 *      “A is the depender, B is the dependee”.
 *          `===`
 *      “B is depended on by A”
 *
 * @note The very core of the framework is described in:
 *       [[https://conf.researchr.org/track/ecoop-issta-2018/SOAP-2018-papers#program Lattice Based Modularization of Static Analyses]]
 *
 * @author Michael Eichberg
 */
package object fpcf {

    final val FrameworkName = "OPAL Static Analyses Infrastructure"

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext: LogContext = GlobalLogContext
        try {
            assert(false)
            // when we reach this point assertions are turned off
            info(FrameworkName, "Production Build")
        } catch {
            case _: AssertionError => info(FrameworkName, "Development Build with Assertions")
        }
    }

    final type AnalysisKey = PropertyKey[Null]

    /**
     * The type of the values stored in a property store.
     *
     * Basically, a simple type alias to facilitate comprehension of the code.
     */
    final type Entity = AnyRef

    final type SomeEOptionP = EOptionP[_ <: Entity, _ <: Property]

    final type SomeEPK = EPK[_ <: Entity, _ <: Property]

    final type SomeEPS = EPS[_ <: Entity, _ <: Property]

    final type SomeFinalEP = FinalEP[_ <: Entity, _ <: Property]

    final type SomeInterimEP = InterimEP[_ <: Entity, _ <: Property]

    final type SomePartialResult = PartialResult[_ >: Null <: Entity, _ >: Null <: Property]

    final type UpdateComputation[E <: Entity, P <: Property] = EOptionP[E, P] => Option[InterimEP[E, P]]

    final type SomeUpdateComputation = UpdateComputation[_ <: Entity, _ <: Property]

    /**
     * A function that takes an entity and returns a result. The result maybe:
     *  - the final derived property,
     *  - a function that will continue computing the result once the information
     *    about some other entity is available or,
     *  - an intermediate result which may be refined later on, but not by the
     *    current running analysis.
     *
     * @note In some cases it makes sense that an analysis processes entities of kind A, but derives
     *       properties related to entities with kind B. E.g., it is possible to have an analysis
     *       that processes entire classes to compute the properties of some fields. This scenario
     *       is, however, only supported by eager analyses.
     */
    final type PropertyComputation[E <: Entity] = E => PropertyComputationResult

    final type SomePropertyComputation = PropertyComputation[_ <: Entity]

    final type ProperPropertyComputation[E <: Entity] = E => ProperPropertyComputationResult

    final type SomeProperPropertyComputation = ProperPropertyComputation[_ <: Entity]

    final type OnUpdateContinuation = SomeEPS => PropertyComputationResult

    final type ProperOnUpdateContinuation = SomeEPS => ProperPropertyComputationResult

    final type QualifiedOnUpdateContinuation[E <: Entity, P <: Property] = EOptionP[E, P] => ProperPropertyComputationResult

    /**
     * The [[FallbackReason]] specifies the reason why a fallback property is required. This
     * information can be used to handle situations where the fallback should be different
     * based on the information whether a corresponding analysis was executed or not.
     */
    final type FallbackPropertyComputation[E <: Entity, P <: Property] = (PropertyStore, FallbackReason, E) => P

    final type SomeFallbackPropertyComputation = FallbackPropertyComputation[_ <: Entity, _ <: Property]

    /**
     * A function that continues the computation of a property. It takes
     * the entity and property of the entity on which the computation depends.
     */
    final type Continuation[P <: Property] = (Entity, P) => PropertyComputationResult

    // final type SomeContinuation = Continuation[_ <: Property]

    final type SomePropertyKey = PropertyKey[_ <: Property]

    /**
     * The result of a computation if the computation derives multiple properties at the same time.
     */
    final type ComputationResults = IterableOnce[SomeFinalEP]

    private[fpcf] final val AnalysisKeyName = "<internal>opalj.PartialResultUpdateComputation"

    private[fpcf] final val AnalysisKey = PropertyKey.create[Entity, Null](AnalysisKeyName)

    private[fpcf] final val AnalysisKeyId = AnalysisKey.id

    def hashCodeToHexString(o: AnyRef): String = {
        System.identityHashCode(o).toHexString
    }

    def anyRefToShortString(o: AnyRef): String = {
        o.getClass.getSimpleName+"@"+hashCodeToHexString(o)
    }

}
