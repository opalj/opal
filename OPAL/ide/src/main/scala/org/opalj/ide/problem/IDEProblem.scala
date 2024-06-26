/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.problem

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.solver.ICFG

/**
 * Interface for modeling IDE problems
 */
abstract class IDEProblem[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
        val icfg: ICFG[Statement, Callable]
) {
    /**
     * The null fact to use. Also used to bootstrap the analysis at the entry points.
     */
    val nullFact: Fact

    /**
     * The lattice that orders the used values
     */
    val lattice: MeetLattice[Value]

    /**
     * Generate a flow function for a normal flow
     * @param source where the normal flow starts
     * @param target where the normal flow ends
     */
    def getNormalFlowFunction(source: Statement, target: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate a flow function for a call flow
     * @param callSite where the call flow starts (always a call statement)
     * @param calleeEntry where the callable starts (the statement which the callable is started with)
     * @param callee the callable that is called
     */
    def getCallFlowFunction(callSite: Statement, calleeEntry: Statement, callee: Callable)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate a flow function for a return flow
     * @param calleeExit where the return flow starts (the statement the callable is exited with)
     * @param callee the callable that is returned from
     * @param returnSite where the return flow ends (e.g. the next statement after the call in the callers code)
     */
    def getReturnFlowFunction(calleeExit: Statement, callee: Callable, returnSite: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate a flow function for a call-to-return flow
     * @param callSite where the call-to-return flow starts (always a call statement)
     * @param callee the callable this flow is about
     * @param returnSite where the call-to-return flow ends (e.g. the next statement after the call)
     */
    def getCallToReturnFlowFunction(callSite: Statement, callee: Callable, returnSite: Statement)(
        implicit propertyStore: PropertyStore
    ): FlowFunction[Fact]

    /**
     * Generate an edge function for a normal flow
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
    )(implicit propertyStore: PropertyStore): EdgeFunction[Value]

    /**
     * Generate an edge function for a call flow
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
    )(implicit propertyStore: PropertyStore): EdgeFunction[Value]

    /**
     * Generate an edge function for a return flow
     * @param calleeExit where the return flow starts (the statement the callable is exited with)
     * @param calleeExitFact the fact the flow starts with
     * @param callee the callable that is returned from
     * @param returnSite where the return flow ends (e.g. the next statement after the call in the callers code)
     * @param returnSiteFact the fact the flow ends with
     */
    def getReturnEdgeFunction(
        calleeExit:     Statement,
        calleeExitFact: Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunction[Value]

    /**
     * Generate an edge function for a call-to-return flow
     * @param callSite where the call-to-return flow starts (always a call statement)
     * @param callSiteFact the fact the flow starts with
     * @param returnSite where the call-to-return flow ends (e.g. the next statement after the call)
     * @param returnSiteFact the fact the flow ends with
     */
    def getCallToReturnEdgeFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunction[Value]
}
