/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

import scala.collection.{Set => SomeSet}

abstract class ICFG[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[C, _]] {
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
   * Gets the set of all methods possibly called at some statement.
   *
   * @param statement The statement.
   * @return All callables possibly called at the statement or None, if the statement does not
   *         contain a call.
   */
  def getCalleesIfCallStatement(statement: S): Option[SomeSet[C]]

  /**
   * Determines whether the statement is an exit statement.
   *
   * @param statement The source statement.
   * @return Whether the statement flow may exit its callable (function/method)
   */
  def isExitStatement(statement: S): Boolean
}
