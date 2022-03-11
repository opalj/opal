/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds
import scala.collection.{Set ⇒ SomeSet}
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSFact, IFDSProblem}

abstract class ICFG[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[Node], Node] {
    type State = IFDSState[IFDSFact, C, S, Node]
    type Problem = IFDSProblem[IFDSFact, C, S]

    /**
     * Determines the basic blocks, at which the analysis starts.
     *
     * @param sourceFact The source fact of the analysis.
     * @param callable The analyzed callable.
     * @return The basic blocks, at which the analysis starts.
     */
    def startNodes(sourceFact: IFDSFact, callable: C): Set[Node]

    /**
     * Determines the nodes, that will be analyzed after some `node`.
     *
     * @param node The basic block, that was analyzed before.
     * @return The nodes, that will be analyzed after `node`.
     */
    def nextNodes(node: Node): Set[Node]

    /**
     * Checks, if some `node` is the last node.
     *
     * @return True, if `node` is the last node, i.e. there is no next node.
     */
    def isLastNode(node: Node): Boolean

    /**
     * Determines the first index of some `basic block`, that will be analyzed.
     *
     * @param node The basic block.
     * @return The first index of some `basic block`, that will be analyzed.
     */
    def firstStatement(node: Node): S

    /**
     * Determines the last index of some `basic block`, that will be analzyed.
     *
     * @param node The basic block.
     * @return The last index of some `basic block`, that will be analzyed.
     */
    def lastStatement(node: Node): S

    /**
     * Determines the statement that will be analyzed after some other statement.
     *
     * @param statement The current statement.
     * @return The statement that will be analyzed after `statement`.
     */
    def nextStatement(statement: S): S

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    def nextStatements(statement: S): Set[S]

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    def getCalleesIfCallStatement(statement: S): Option[SomeSet[C]]

    /**
     * Collects the facts valid at all exit nodes based on the current results.
     *
     * @return A map, mapping from each predecessor of all exit nodes to the facts, which hold at
     *         the exit node under the assumption that the predecessor was executed before.
     */
    def collectResult(implicit state: State): Map[S, Set[IFDSFact]]

    /**
     * Determines the facts, for which a `callee` is analyzed.
     *
     * @param call The call, which calls `callee`.
     * @param callee The method, which is called by `call`.
     * @param in The facts, which hold before the `call`.
     * @return The facts, for which `callee` will be analyzed.
     */
    def callToStartFacts(call: S, callee: C, in: Set[IFDSFact])(
        implicit
        state:       State,
        ifdsProblem: Problem,
        statistics:  Statistics
    ): Set[IFDSFact]

    /**
     * Collects the exit facts of a `callee` and adds them to the `summaryEdges`.
     *
     * @param summaryEdges The current summary edges. They map successor statements of the `call`
     *                     to facts, which hold before they are executed.
     * @param successors The successor of `call`, which is considered.
     * @param call The statement, which calls `callee`.
     * @param callee The method, called by `call`.
     * @param exitFacts Maps exit statements of the `callee` to the facts, which hold after them.
     * @return The summary edges plus the exit to return facts for `callee` and `successor`.
     */
    def addExitToReturnFacts(
        summaryEdges: Map[S, Set[IFDSFact]],
        successors:   Set[S],
        call:         S,
        callee:       C,
        exitFacts:    Map[S, Set[IFDSFact]]
    )(implicit state: State, ifdsProblem: Problem): Map[S, Set[IFDSFact]]
}
