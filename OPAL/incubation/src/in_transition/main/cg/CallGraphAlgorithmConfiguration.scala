/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses.cg

import org.opalj.br.analyses.SomeProject

/**
 * Configuration of a specific
 * [[org.opalj.ai.analyses.cg.CallGraphExtractor]] call graph algorithm.
 * Basically, the configuration consist of the `Cache` object that will be used during the
 * computation of the call graph and the extractor that will be used
 * for each method that is analyzed during the construction of the graph.
 *
 * @author Michael Eichberg
 */
trait CallGraphAlgorithmConfiguration {

    /**
     * The contour identifies the key of the CallGraphCache.
     */
    protected type Contour

    /**
     * The type of the cached values.
     */
    protected type Value

    protected type Cache <: CallGraphCache[Contour, Value]

    val project: SomeProject

    /**
     * Creates a new cache that is used to cache intermediate results while
     * computing the call graph.
     *
     * Usually created only once per run.
     */
    protected[this] val cache: Cache

    val Extractor: CallGraphExtractor
}
