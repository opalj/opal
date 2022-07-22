/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds.old

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.ifds.{AbstractIFDSFact, IFDSPropertyMetaInformation, Statement}

/**
 * A framework for IFDS analyses.
 *
 * @tparam IFDSFact The type of flow facts, which are tracked by the concrete analysis.
 * @author Dominik Helm
 * @author Mario Trageser
 */
abstract class IFDSProblem[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[_, Node], Node](val icfg: ICFG[IFDSFact, C, S, Node]) {
    /**
     * The null fact of this analysis.
     */
    def nullFact: IFDSFact

    /**
     * The entry points of this analysis.
     */
    def entryPoints: Seq[(C, IFDSFact)]

    /**
     * Computes the data flow for a normal statement.
     *
     * @param statement The analyzed statement.
     * @param successor The successor of the analyzed `statement`, for which the data flow shall be
     *                  computed. It is not present for exit statements.
     * @param in The facts, which hold before the execution of the `statement`.
     * @return The facts, which hold after the execution of `statement` under the assumption
     *         that the facts in `in` held before `statement` and `successor` will be
     *         executed next.
     */
    def normalFlow(
        statement: S,
        successor: Option[S],
        in:        Set[IFDSFact]
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in The facts, which hold before the execution of the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    def callFlow(
        call:   S,
        callee: C,
        in:     Set[IFDSFact],
        source: (C, IFDSFact)
    ): Set[IFDSFact]

    /**
     * Computes the data flow for an exit to return edge.
     *
     * @param call The statement, which called the `callee`.
     * @param callee The method called by `call`, for which the data flow shall be computed.
     * @param exit The statement, which terminated the `calle`.
     * @param successor The statement of the caller, which will be executed after the `callee`
     *                  returned.
     * @param in The facts, which hold before the execution of the `exit`.
     * @return The facts, which hold after the execution of `exit` in the caller's context
     *         under the assumption that `in` held before the execution of `exit` and that
     *         `successor` will be executed next.
     */
    def returnFlow(
        call:      S,
        callee:    C,
        exit:      S,
        successor: S,
        in:        Set[IFDSFact]
    ): Set[IFDSFact]

    /**
     * Computes the data flow for a call to return edge.
     *
     * @param call The statement, which invoked the call.
     * @param successor The statement, which will be executed after the call.
     * @param in The facts, which hold before the `call`.
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the call independently of what happens in the callee
     *         under the assumption that `in` held before `call`.
     */
    def callToReturnFlow(
        call:      S,
        successor: S,
        in:        Set[IFDSFact],
        source:    (C, IFDSFact)
    ): Set[IFDSFact]

    type OutsideAnalysisContextHandler = ((S, S, Set[IFDSFact]) => Set[IFDSFact]) {
        def apply(call: S, successor: S, in: Set[IFDSFact]): Set[IFDSFact]
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

    def specialCase(source: (C, IFDSFact), propertyKey: IFDSPropertyMetaInformation[S, IFDSFact]): Option[ProperPropertyComputationResult] = None
}