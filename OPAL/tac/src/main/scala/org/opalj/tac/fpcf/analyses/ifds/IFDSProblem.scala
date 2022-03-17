/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.{Entity, ProperPropertyComputationResult}
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * The supertype of all IFDS facts.
 */
trait AbstractIFDSFact

/**
 * The super type of all null facts.
 */
trait AbstractIFDSNullFact extends AbstractIFDSFact

/**
 * A framework for IFDS analyses.
 *
 * @tparam IFDSFact The type of flow facts, which are tracked by the concrete analysis.
 * @author Dominik Helm
 * @author Mario Trageser
 */
abstract class IFDSProblem[IFDSFact <: AbstractIFDSFact, C, Statement](val project: SomeProject) {
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
        statement: Statement,
        successor: Option[Statement],
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
        call:   Statement,
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
        call:      Statement,
        callee:    C,
        exit:      Statement,
        successor: Statement,
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
        call:      Statement,
        successor: Statement,
        in:        Set[IFDSFact],
        source:    (C, IFDSFact)
    ): Set[IFDSFact]

    /**
     * Checks, if a callee is inside this analysis' context.
     * If not, `callOutsideOfAnalysisContext` is called instead of analyzing the callee.
     * By default, native methods are not inside the analysis context.
     *
     * @param callee The callee.
     * @return True, if the callee is inside the analysis context.
     */
    def insideAnalysisContext(callee: C): Boolean

    /**
     * When a callee outside of this analysis' context is called, this method computes the summary
     * edge for the call.
     *
     * @param call The statement, which invoked the call.
     * @param callee The method called by `call`.
     * @param successor The statement, which will be executed after the call.
     * @param in The facts facts, which hold before the `call`.
     * @return The facts, which hold after the call, excluding the call to return flow.
     */
    def callOutsideOfAnalysisContext(
        call:      Statement,
        callee:    C,
        successor: Statement,
        in:        Set[IFDSFact]
    ): Set[IFDSFact]

    /**
     * Gets the set of all methods directly callable at some call statement.
     *
     * @param statement The call statement.
     * @param caller The caller, performing the call.
     * @return All methods directly callable at the statement index.
     */
    def getCallees(
        statement: Statement,
        caller:    C
    ): Iterator[C]

    def delegateAnalysis(source: (C, IFDSFact)): Option[((Entity, IFDSFact), Set[IFDSFact] ⇒ Set[IFDSFact])] = None
    def specialCase(source: (C, IFDSFact), propertyKey: IFDSPropertyMetaInformation[Statement, IFDSFact]): Option[ProperPropertyComputationResult] = None
}

/**
 * A data flow fact, that was created by an unbalanced return.
 *
 * @tparam FactType The type of flow facts, which are tracked by the concrete analysis.
 */
trait UnbalancedReturnFact[FactType] extends AbstractIFDSFact {

    /**
     * The index of the call, which creates the inner fact.
     */
    val index: Int

    /**
     * The fact, which will hold after `index`.
     */
    val innerFact: FactType
}

trait BackwardIFDSProblem[IFDSFact <: AbstractIFDSFact, UnbalancedIFDSFact <: IFDSFact with UnbalancedReturnFact[IFDSFact], Method, Statement] {
    /**
     * Checks for the analyzed entity, if an unbalanced return should be performed.
     *
     * @param source The analyzed entity.
     * @return False, if no unbalanced return should be performed.
     */
    def shouldPerformUnbalancedReturn(source: (Method, IFDSFact)): Boolean

    /**
     * Called, when the entry point of the analyzed method is reached.
     * Creates unbalanced return facts for the callers.
     *
     * @param facts The facts, which hold at the entry point of the analyzed method.
     * @param call A call, which calls the analyzed method.
     * @param caller The method, invoking the `call`.
     * @param source The entity, which is currently analyzed.
     * @return Unbalanced return facts, that hold after `call` under the assumption, that `facts`
     *         held at the entry point of the analyzed method.
     */
    def unbalancedReturnFlow(
        facts:  Set[IFDSFact],
        call:   Statement,
        caller: Method,
        source: (Method, IFDSFact)
    ): Set[UnbalancedIFDSFact]

    /**
     * Called, when the analysis found new output facts at a method's start block.
     * A concrete analysis may overwrite this method to create additional facts, which will be added
     * to the analysis' result.
     *
     * @param in The new output facts at the start node.
     * @param source The entity, which is analyzed.
     * @return Nothing by default.
     */
    def createFactsAtStartNode(
        in:     Set[IFDSFact],
        source: (Method, IFDSFact)
    ): Set[IFDSFact] =
        Set.empty
}