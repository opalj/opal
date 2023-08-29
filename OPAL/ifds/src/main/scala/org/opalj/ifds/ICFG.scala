/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ifds

import scala.collection.{Set => SomeSet}

/**
 * The interface of the ICFG for the IFDS Analysis
 *
 * @tparam C The type of callables in this ICFG
 * @tparam S the type of statements in this ICFG
 *
 * @author Marc Clement
 */
abstract class ICFG[C <: AnyRef, S <: Statement[_ <: C, _]] {
    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    def startStatements(callable: C): Set[S]

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    def nextStatements(statement: S): Set[S]

    /**
     * Gets the set of all callables possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    def getCalleesIfCallStatement(statement: S): Option[SomeSet[_ <: C]]

    /**
     * Determines whether the statement is an exit statement.
     *
     * @param statement The source statement.
     * @return Whether the statement flow may exit its callable (function/method)
     */
    def isExitStatement(statement: S): Boolean

    /**
     * Get all instances where a function is called.
     *
     * @param callee the function whose callers should be found.
     * @return a seq of statements where the callee is called.
     */
    def getCallers(callee: C): Set[S]
}

/**
 * The representation of statements in the icfg
 *
 * @tparam C type of callables
 * @tparam Node type of Basic Blocks
 */
abstract class Statement[C, Node] {
    def basicBlock: Node
    def callable: C
}

/**
 * Represents Callables in the ICFG for the IFDS Analysis.
 *
 * @author Marc Clement
 */
abstract class Callable {
    /**
     * The name of the Callable
     */
    def name: String

    /**
     * The full name of the Callable including its signature
     */
    def signature: String
}
