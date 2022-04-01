/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.old

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.cfg.CFGNode
import org.opalj.fpcf.{FinalEP, PropertyStore}
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.old.ICFG
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.cg.Callees

class ForwardICFG[IFDSFact <: AbstractIFDSFact](implicit
        propertyStore: PropertyStore,
                                                typeProvider:    TypeProvider,
                                                declaredMethods: DeclaredMethods
) extends ICFG[IFDSFact, DeclaredMethod, DeclaredMethodJavaStatement, CFGNode] {
    /**
     * Determines the basic blocks, at which the analysis starts.
     *
     * @param sourceFact The source fact of the analysis.
     * @param callable   The analyzed callable.
     * @return The basic blocks, at which the analysis starts.
     */
    override def startNodes(sourceFact: IFDSFact, callable: DeclaredMethod): Set[CFGNode] = ???

    /**
     * Determines the nodes, that will be analyzed after some `basicBlock`.
     *
     * @param node The basic block, that was analyzed before.
     * @return The nodes, that will be analyzed after `basicBlock`.
     */
    override def nextNodes(node: CFGNode): Set[CFGNode] = ???

    /**
     * Checks, if some `node` is the last node.
     *
     * @return True, if `node` is the last node, i.e. there is no next node.
     */
    override def isLastNode(node: CFGNode): Boolean = ???

    /**
     * Determines the first index of some `basic block`, that will be analyzed.
     *
     * @param basicBlock The basic block.
     * @return The first index of some `basic block`, that will be analyzed.
     */
    override def firstStatement(basicBlock: CFGNode): DeclaredMethodJavaStatement = ???

    /**
     * Determines the last index of some `basic block`, that will be analzyed.
     *
     * @param basicBlock The basic block.
     * @return The last index of some `basic block`, that will be analzyed.
     */
    override def lastStatement(basicBlock: CFGNode): DeclaredMethodJavaStatement = ???

    /**
     * Determines the statement that will be analyzed after some other statement.
     *
     * @param statement The current statement.
     * @return The statement that will be analyzed after `statement`.
     */
    override def nextStatement(statement: DeclaredMethodJavaStatement): DeclaredMethodJavaStatement = ???

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: DeclaredMethodJavaStatement): Set[DeclaredMethodJavaStatement] = ???

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: DeclaredMethodJavaStatement): Option[collection.Set[DeclaredMethod]] = {
        val pc = statement.code(statement.index).pc
        val caller = declaredMethods(statement.method)
        val ep = propertyStore(caller, Callees.key)
        ep match {
            case FinalEP(_, p) ⇒ Some(p.directCallees(typeProvider.newContext(caller), pc).map(_.method).toSet)
            case _ ⇒
                throw new IllegalStateException(
                    "call graph mut be computed before the analysis starts"
                )
        }
    }

    override def isExitStatement(statement: DeclaredMethodJavaStatement): Boolean = ???
}
