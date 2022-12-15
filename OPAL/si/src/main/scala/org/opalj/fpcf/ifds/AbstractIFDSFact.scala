/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds

/**
 * The supertype of all IFDS facts, may implement "subsumes" to enable subsuming.
 */
trait AbstractIFDSFact

/**
 * The super type of all null facts.
 */
trait AbstractIFDSNullFact extends AbstractIFDSFact
