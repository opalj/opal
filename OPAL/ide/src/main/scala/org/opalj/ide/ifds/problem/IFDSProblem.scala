/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package ifds
package problem

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.EdgeFunctionResult
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.MeetLattice

/**
 * Interface for modeling IFDS problems based on an IDE problem
 */
abstract class IFDSProblem[Fact <: IDEFact, Statement, Callable <: Entity]
    extends IDEProblem[Fact, IFDSValue, Statement, Callable] {
    override final val lattice: MeetLattice[IFDSValue] = IFDSLattice

    override final def getNormalEdgeFunction(
        source:     Statement,
        sourceFact: Fact,
        target:     Statement,
        targetFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[IFDSValue] = {
        if (sourceFact == nullFact) {
            AllBottomEdgeFunction
        } else {
            identityEdgeFunction
        }
    }

    override final def getCallEdgeFunction(
        callSite:        Statement,
        callSiteFact:    Fact,
        calleeEntry:     Statement,
        calleeEntryFact: Fact,
        callee:          Callable
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[IFDSValue] = {
        if (callSiteFact == nullFact) {
            AllBottomEdgeFunction
        } else {
            identityEdgeFunction
        }
    }

    override final def getReturnEdgeFunction(
        calleeExit:     Statement,
        calleeExitFact: Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact,
        callSite:       Statement,
        callSiteFact:   Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[IFDSValue] = {
        if (calleeExitFact == nullFact) {
            AllBottomEdgeFunction
        } else {
            identityEdgeFunction
        }
    }

    override final def getCallToReturnEdgeFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[IFDSValue] = {
        if (callSiteFact == nullFact) {
            AllBottomEdgeFunction
        } else {
            identityEdgeFunction
        }
    }

    /**
     * Whether precomputed flow functions for a `(callSite, callSiteFact, callee)` combination exist (resp. can be
     * generated).
     * @param callSite where the flow starts
     * @param callSiteFact the fact the flow starts with
     * @param callee the callable this flow is about
     */
    def hasPrecomputedFlowFunction(callSite: Statement, callSiteFact: Fact, callee: Callable)(
        implicit propertyStore: PropertyStore
    ): Boolean = {
        false
    }

    override final def hasPrecomputedFlowAndSummaryFunction(
        callSite:     Statement,
        callSiteFact: Fact,
        callee:       Callable
    )(implicit propertyStore: PropertyStore): Boolean = {
        hasPrecomputedFlowFunction(callSite, callSiteFact, callee)
    }

    override final def getPrecomputedSummaryFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        callee:         Callable,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[IFDSValue] = {
        if (callSiteFact == nullFact) {
            AllBottomEdgeFunction
        } else {
            identityEdgeFunction
        }
    }

    override final def getPrecomputedSummaryFunction(
        callSite:       Statement,
        callSiteFact:   Fact,
        returnSite:     Statement,
        returnSiteFact: Fact
    )(implicit propertyStore: PropertyStore): EdgeFunctionResult[IFDSValue] = {
        if (callSiteFact == nullFact) {
            AllBottomEdgeFunction
        } else {
            identityEdgeFunction
        }
    }
}
