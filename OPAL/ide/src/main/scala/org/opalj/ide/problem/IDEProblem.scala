/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package problem

import scala.annotation.unused
import scala.language.implicitConversions

import scala.collection
import scala.collection.immutable

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore

/**
 * Interface for modeling IDE problems.
 *
 * @author Robin Körkemeier
 */
abstract class IDEProblem[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity] {
    implicit def edgeFunctionToFinalEdgeFunction(edgeFunction: EdgeFunction[Value]): EdgeFunctionResult[Value] = {
        FinalEdgeFunction(edgeFunction)
    }

    /**
     * Empty flow function that can be used when implementing problems
     */
    protected val emptyFlowFunction = new EmptyFlowFunction[Fact]

    /**
     * Identity edge function that can be used when implementing problems
     */
    protected val identityEdgeFunction = new IdentityEdgeFunction[Value]

    /**
     * The null fact to use. Also used to bootstrap the analysis at the entry points.
     */
    val nullFact: Fact

    /**
     * The lattice that orders the used values
     */
    val lattice: MeetLattice[Value]

    /**
     * Add additional facts that the analysis should be seeded with. Traditionally, IDE starts with the null fact at the
     * start statements of the callable. E.g. additional seeds can be used for adding facts about the parameters of the
     * analyzed callable.
     *
     * @param stmt the start statement
     * @param callee the analyzed callable
     */
    def getAdditionalSeeds(stmt: Statement, callee: Callable)(
        implicit @unused propertyStore: PropertyStore
    ): collection.Set[Fact] = immutable.Set.empty

    /**
     * Generate an edge function for a flow starting with an additional seeds.
     *
     * @param stmt the start statement
     * @param fact the start fact
     * @param callee the analyzed callable
     */
    def getAdditionalSeedsEdgeFunction(stmt: Statement, fact: Fact, callee: Callable)(
        implicit @unused propertyStore: PropertyStore
    ): EdgeFunctionResult[Value] = identityEdgeFunction

    /**
     * Generate a flow function for a normal flow.
     *
     * @param source where the normal flow starts
     * @param sourceFact the fact the flow starts with
     * @param target where the normal flow ends
     */
    def getNormalFlowFunction(source: Statement, sourceFact: Fact, target: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate a flow function for a call flow.
     *
     * @param callSite where the call flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param calleeEntry where the callable starts (the statement which the callable is started with)
     * @param callee the callable that is called
     */
    def getCallFlowFunction(callSite: Statement, callSiteFact: Fact, calleeEntry: Statement, callee: Callable)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate a flow function for a return flow.
     *
     * @param calleeExit where the return flow starts (the statement the callable is exited with)
     * @param calleeExitFact the fact the flow starts with
     * @param callee the callable that is returned from
     * @param returnSite where the return flow ends (e.g. the next statement after the call in the callers code)
     * @param callSite corresponding to the return flow
     * @param callSiteFact corresponding to the return flow
     */
    def getReturnFlowFunction(
        calleeExit:     Statement,
        calleeExitFact: Fact,
        callee:         Callable,
        returnSite:     Statement,
        callSite:       Statement,
        callSiteFact:   Fact
    )(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate a flow function for a call-to-return flow.
     *
     * @param callSite where the call-to-return flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param callee the callable this flow is about
     * @param returnSite where the call-to-return flow ends (e.g. the next statement after the call)
     */
    def getCallToReturnFlowFunction(callSite: Statement, callSiteFact: Fact, callee: Callable, returnSite: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate an edge function for a normal flow.
     *
     * @param source where the normal flow starts
     * @param sourceFact the fact the flow starts with
     * @param target where the normal flow ends
     * @param targetFact the fact the flow ends with
     */
    def getNormalEdgeFunction(
        source:     Statement,
        sourceFact: Fact,
        target:     Statement,
        targetFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value]

    /**
     * Generate an edge function for a call flow.
     *
     * @param callSite where the call flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param calleeEntry where the callable starts (the statement which the callable is started with)
     * @param calleeEntryFact the fact the flow ends with
     * @param callee the callable that is called
     */
    def getCallEdgeFunction(
        callSite:        Statement,
        callSiteFact:    Fact,
        calleeEntry:     Statement,
        calleeEntryFact: Fact,
        callee:          Callable
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value]

    /**
     * Generate an edge function for a return flow.
     *
     * @param calleeExit where the return flow starts (the statement the callable is exited with)
     * @param calleeExitFact the fact the flow starts with
     * @param callee the callable that is returned from
     * @param returnSite where the return flow ends (e.g. the next statement after the call in the callers code)
     * @param returnSiteFact the fact the flow ends with
     * @param callSite corresponding to the return flow
     * @param callSiteFact corresponding to the return flow
     */
    def getReturnEdgeFunction(
        calleeExit:     Statement,
        calleeExitFact: Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact,
        callSite:       Statement,
        callSiteFact:   Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value]

    /**
     * Generate an edge function for a call-to-return flow.
     *
     * @param callSite where the call-to-return flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param callee the callable this flow is about
     * @param returnSite where the call-to-return flow ends (e.g. the next statement after the call)
     * @param returnSiteFact the fact the flow ends with
     */
    def getCallToReturnEdgeFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value]

    /**
     * Whether precomputed flow and summary functions for a `(callSite, callSiteFact, callee)` combination exist
     * (resp. can be generated).
     *
     * @param callSite where the flow starts
     * @param callSiteFact the fact the flow starts with
     * @param callee the callable this flow is about
     */
    def hasPrecomputedFlowAndSummaryFunction(callSite: Statement, callSiteFact: Fact, callee: Callable)(
        implicit propertyStore: PropertyStore
    ): Boolean = {
        false
    }

    /**
     * Generate a flow function that yields the facts that are valid when going through the callable and reaching the
     * return site. Similar to a call-to-return flow (cfg. [[getCallToReturnFlowFunction]]) but capturing the effects
     * that flow through the callable.
     *
     * @param callSite where the flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param callee the callable this flow is about
     * @param returnSite where the flow ends (e.g. the next statement after the call)
     * @note In this type of precomputed flow the callable is known. Thus, the call-to-return flow can be applied
     *       normally and does not need to be integrated in this flow.
     */
    def getPrecomputedFlowFunction(callSite: Statement, callSiteFact: Fact, callee: Callable, returnSite: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact] = {
        throw new IllegalArgumentException(
            s"No precomputed flow function for callSite=$callSite, callSiteFact=$callSiteFact, callee=$callee and " +
                s"returnSite=$returnSite exists!"
        )
    }

    /**
     * Generate a summary function from a call-site node up to a return-site node (just what summary functions are in
     * the foundation paper, but in one step).
     *
     * @param callSite where the flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param callee the callable the flow is about
     * @param returnSite where the flow ends (e.g. the next statement after the call)
     * @param returnSiteFact the fact the flow ends with
     * @note In this type of precomputed flow the callable is known. Thus, the call-to-return flow can be applied
     *       normally and does not need to be integrated in this flow.
     */
    def getPrecomputedSummaryFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value] = {
        throw new IllegalArgumentException(
            s"No precomputed summary function for callSite=$callSite, callSiteFact=$callSiteFact, " +
                s"callee=$callee, returnSite=$returnSite and returnSiteFact=$returnSiteFact exists!"
        )
    }

    /**
     * Generate a flow function that yields the facts that are valid when going through the unknown callable and
     * reaching the return site. Similar to a call-to-return flow (cfg. [[getCallToReturnFlowFunction]]) but capturing
     * the effects that flow through the possible callables.
     *
     * @param callSite where the flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param returnSite where the flow ends (e.g. the next statement after the call)
     * @note In this type of precomputed flow the callable is unknown. Thus, the call-to-return flow is not applied and
     *       needs to be integrated into this flow.
     */
    def getPrecomputedFlowFunction(callSite: Statement, callSiteFact: Fact, returnSite: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact] = {
        throw new IllegalArgumentException(
            s"No precomputed flow function for callSite=$callSite, callSiteFact=$callSiteFact and " +
                s"returnSite=$returnSite exists!"
        )
    }

    /**
     * Generate a summary function from a call-site node up to a return-site node (just what summary functions are in
     * the foundation paper, but in one step and for all callables that are possible call targets).
     *
     * @param callSite where the flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param returnSite where the flow ends (e.g. the next statement after the call)
     * @param returnSiteFact the fact the flow ends with
     * @note In this type of precomputed flow the callable is unknown. Thus, the call-to-return flow is not applied and
     *       needs to be integrated into this flow.
     */
    def getPrecomputedSummaryFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[Value] = {
        throw new IllegalArgumentException(
            s"No precomputed summary function for callSite=$callSite, callSiteFact=$callSiteFact, " +
                s"returnSite=$returnSite and returnSiteFact=$returnSiteFact exists!"
        )
    }
}
