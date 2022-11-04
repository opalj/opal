/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

import org.opalj.ifds.Dependees.Getter

/**
 * A framework for IFDS analyses.
 *
 * @tparam Fact The type of flow facts, which are tracked by the concrete analysis.
 * @author Dominik Helm
 * @author Mario Trageser
 * @author Marc Clement
 */
abstract class IFDSProblem[Fact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[_ <: C, _]](val icfg: ICFG[C, S]) {
    type Work = (S, Fact, Option[S])

    /**
     * The null fact of this analysis.
     */
    def nullFact: Fact

    /**
     * @return Whether the null Fact is automatically added to the result of every flow function where it is passed into
     */
    def automaticallyPropagateNullFactInFlowFunctions: Boolean = true

    /**
     * @return Whether to try to subsume new facts under existing facts and save graph edges
     */
    def subsumeFacts: Boolean = false

    /**
     * The entry points of this analysis.
     */
    def entryPoints: Seq[(C, IFDSFact[Fact, C])]

    /**
     * @return Whether the analysis should follow unbalanced return flows
     *         (return of a method for which no matching previous call was processed).
     */
    def followUnbalancedReturns: Boolean = false

    /**
     * Computes the data flow for a normal statement.
     *
     * @param statement   The analyzed statement.
     * @param in          The fact which holds before the execution of the `statement`.
     * @param predecessor The predecessor of the analyzed `statement`, for which the data flow shall be
     *                    computed. Used for phi statements to distinguish the flow.
     * @return The facts, which hold after the execution of `statement` under the assumption
     *         that the facts in `in` held before `statement` and `successor` will be
     *         executed next.
     */
    def normalFlow(
        statement:   S,
        in:          Fact,
        predecessor: Option[S]
    ): Set[Fact]

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call   The statement, which called the `callee`.
     * @param in     The fact which holds before the execution of the `call`.
     * @param callee The called method, for which the data flow shall be computed.
     * @param entry  The entry statement of the 'callee'.
     * @return The facts, which hold after the execution of `call` under the assumption that
     *         the fact `in` held before `call` and `call` calls `callee`.
     */
    def callFlow(entry: S, in: Fact, call: S, callee: C): Set[Fact]

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param exit     The statement, which terminated the `callee`.
     * @param in       The fact which holds before the execution of the `exit`.
     * @param call     The statement, which called the `callee`.
     * @param successor The successor statement of the call, might be None if unbalanced return.
     * @param unbCallChain The current call chain of unbalanced returns.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    def returnFlow(exit: S, in: Fact, call: S, successor: Option[S], unbCallChain: Seq[C]): Set[Fact]

    /**
     * Computes the data flow for a call to return edge.
     *
     * @param call The statement, which invoked the call.
     * @param in   The facts, which hold before the `call`.
     * @param unbCallChain The current call chain of unbalanced returns.
     * @return The facts, which hold after the call independently of what happens in the callee
     *         under the assumption that `in` held before `call`.
     */
    def callToReturnFlow(
        call:         S,
        in:           Fact,
        successor:    Option[S],
        unbCallChain: Seq[C]
    ): Set[Fact]

    def needsPredecessor(statement: S): Boolean

    type OutsideAnalysisContextHandler = ((S, Option[S], Fact, Getter) => Set[Fact]) {
        def apply(call: S, successor: Option[S], in: Fact, dependeesGetter: Getter): Set[Fact]
    }

    /**
     * Checks, if a callee is outside this analysis' context.
     * By default, native methods are not inside the analysis context.
     * For callees outside this analysis' context the returned handler is called
     * to compute the summary edge for the call instead of analyzing the callee.
     *
     * @param callee The method called by `call`.
     * @return The handler function. It receives
     *         the statement which invoked the call,
     *         the successor statement, which will be executed after the call and
     *         the set of input facts which hold before the `call`.
     *         It returns facts, which hold after the call, excluding the call to return flow.
     */
    def outsideAnalysisContext(callee: C): Option[OutsideAnalysisContextHandler]
}
