/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.solver

import org.opalj.fpcf.Entity

/**
 * Interface representing the interprocedural control flow graph
 */
trait ICFG[Statement, Callable <: Entity] {
    /**
     * Get all statements a callable can be entered at
     */
    def getStartStatements(callable: Callable): collection.Set[Statement]

    /**
     * Get all statements that can directly follow the given one
     */
    def getNextStatements(stmt: Statement): collection.Set[Statement]

    /**
     * Check whether a statement exits a callable in a normal way (e.g. with a return)
     */
    def isNormalExitStatement(stmt: Statement): Boolean

    /**
     * Check whether a statement exits a callable in an abnormal way (e.g. by throwing an exception)
     */
    def isAbnormalExitStatement(stmt: Statement): Boolean

    /**
     * Check whether a statement is a call statement
     */
    def isCallStatement(stmt: Statement): Boolean

    /**
     * Get all possible callees a call statement could call
     */
    def getCalleesIfCallStatement(stmt: Statement): Option[collection.Set[? <: Callable]]

    /**
     * Get the callable a statement belongs to
     */
    def getCallable(stmt: Statement): Callable

    /**
     * Build a string representation of a statement. Only used for debugging purposes!
     * @param indent to use on newlines (e.g. indentation for prettier logs)
     * @param short whether to build a long or a more compact string
     */
    def stringifyStatement(stmt: Statement, indent: String = "", short: Boolean = false): String
}
