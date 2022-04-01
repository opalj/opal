/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

import org.opalj.ifds.Dependees.Getter

/**
 * A framework for IFDS analyses.
 *
 * @tparam IFDSFact The type of flow facts, which are tracked by the concrete analysis.
 * @author Dominik Helm
 * @author Mario Trageser
 * @author Marc Clement
 */
abstract class IFDSProblem[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[C, _]](val icfg: ICFG[IFDSFact, C, S]) {
    type Work = (S, IFDSFact, Option[S])

    /**
     * The null fact of this analysis.
     */
    def nullFact: IFDSFact

    /**
     * @return Whether the null Fact is automatically added to the result of every flow function where it is passed into
     */
    def automaticallyPropagateNullFactInFlowFunctions: Boolean = true

    /**
     * The entry points of this analysis.
     */
    def entryPoints: Seq[(C, IFDSFact)]

    /**
     * Computes the data flow for a normal statement.
     *
     * @param statement The analyzed statement.
     * @param in The fact which holds before the execution of the `statement`.
     * @param predecessor The predecessor of the analyzed `statement`, for which the data flow shall be
     *                  computed. Used for phi statements to distinguish the flow.
     * @return The facts, which hold after the execution of `statement` under the assumption
     *         that the facts in `in` held before `statement` and `successor` will be
     *         executed next.
     */
    def normalFlow(
        statement:   S,
        in:          IFDSFact,
        predecessor: Option[S]
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in The fact which holds before the execution of the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    def callFlow(
        call:   S,
        callee: C,
        in:     IFDSFact
    ): Set[IFDSFact]

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param exit The statement, which terminated the `callee`.
     * @param in The fact which holds before the execution of the `exit`.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    def returnFlow(
        exit:     S,
        in:       IFDSFact,
        call:     S,
        callFact: IFDSFact
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a call to return edge.
     *
     * @param call The statement, which invoked the call.
     * @param in The facts, which hold before the `call`.
     * @return The facts, which hold after the call independently of what happens in the callee
     *         under the assumption that `in` held before `call`.
     */
    def callToReturnFlow(
        call: S,
        in:   IFDSFact
    ): Set[IFDSFact]

    def needsPredecessor(statement: S): Boolean

    type OutsideAnalysisContextHandler = ((S, S, IFDSFact, Getter) ⇒ Set[IFDSFact]) {
        def apply(call: S, successor: S, in: IFDSFact, dependeesGetter: Getter): Set[IFDSFact]
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