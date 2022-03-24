/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

import org.opalj.br.analyses.SomeProject

/**
 * The supertype of all IFDS facts.
 */
trait AbstractIFDSFact

/**
 * The super type of all null facts.
 */
trait AbstractIFDSNullFact extends AbstractIFDSFact

/**
 * The supertype of all IFDS facts, which can subsume another fact.
 */
trait SubsumableFact extends AbstractIFDSFact {

  /**
   * Checks, if this fact subsumes an `other` fact.
   *
   * @param other The other fact.
   * @param project The analyzed project.
   * @return True, if this fact subsumes the `other`fact
   */
  def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean
}

/**
 * The null fact for subsumable facts.
 */
trait SubsumableNullFact extends SubsumableFact with AbstractIFDSNullFact {

  /**
   * The null fact cannot subsume another fact.
   */
  override def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean = false
}