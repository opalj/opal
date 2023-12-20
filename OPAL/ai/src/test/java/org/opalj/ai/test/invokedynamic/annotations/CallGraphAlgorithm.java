/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.test.invokedynamic.annotations;

/**
 * @author Michael Reif
 * @author Michael Eichberg
 */
public enum CallGraphAlgorithm {

    // defined by increasing order of precision
    /** Class Hierarchy Analysis */
    CHA,
    /** Variable Type Analysis */
    BasicVTA,
    /**
     * Variable Type Analysis with field and return type refinement and local reference
     * values tracking
     */
    DefaultVTA,
    /**
     * Variable Type Analysis with field and return type refinement and local values
     * tracking.
     */
    ExtVTA,
    /**
     * Context-sensitive Variable Type Analysis with field and return type refinement and local reference
     * values tracking.
     */
    CFA
}
