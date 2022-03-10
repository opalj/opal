/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds
import scala.collection.{Set ⇒ SomeSet}
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSFact

abstract class ICFG[IFDSFact <: AbstractIFDSFact, C, Statement, Node] {
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
    def firstStatement(node: Node): Statement

    /**
     * Determines the last index of some `basic block`, that will be analzyed.
     *
     * @param node The basic block.
     * @return The last index of some `basic block`, that will be analzyed.
     */
    def lastStatement(node: Node): Statement

    /**
     * Determines the statement that will be analyzed after some other statement.
     *
     * @param statement The current statement.
     * @return The statement that will be analyzed after `statement`.
     */
    def nextStatement(statement: Statement): Statement

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    def nextStatements(statement: Statement): Set[Statement]

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    def getCalleesIfCallStatement(statement: Statement): Option[SomeSet[C]]
}
