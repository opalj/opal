/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.ifds.AbstractIFDSFact

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
