/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ifds

import org.opalj.br.analyses.SomeProject

/**
 * The supertype of all IFDS facts, may implement "subsumes" to enable subsumption.
 *
 * @author Marc Clement
 */
trait AbstractIFDSFact {
    /**
     * Checks if this fact subsumes an `other` fact.
     *
     * @param other The other fact.
     * @param project The analyzed project.
     * @return True if this fact subsumes the `other`fact
     */
    def subsumes(other: AbstractIFDSFact, project: SomeProject): Boolean = false
}

/**
 * The super type of all null facts.
 */
trait AbstractIFDSNullFact extends AbstractIFDSFact
