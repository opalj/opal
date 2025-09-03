/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package solver

import org.opalj.fpcf.Entity

/**
 * Interface representing the interprocedural control flow graph.
 *
 * @author Robin Körkemeier
 */
trait ICFG[Statement, Callable <: Entity] {
    /**
     * Get all statements a callable can be entered at
     */
    def getStartStatements(callable: Callable): Set[Statement]

    /**
     * Get all statements that can directly follow the given one
     */
    def getNextStatements(stmt: Statement): Set[Statement]

    /**
     * Check whether a statement exits a callable (in a normal or abnormal way)
     */
    def isExitStatement(stmt: Statement): Boolean = {
        isNormalExitStatement(stmt) || isAbnormalExitStatement(stmt)
    }

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
    def getCallees(stmt: Statement): Set[Callable]

    /**
     * Get the callable a statement belongs to
     */
    def getCallable(stmt: Statement): Callable

    /**
     * Get all possible statements that could call a callable
     */
    def getCallers(callable: Callable): Set[Statement]
}
