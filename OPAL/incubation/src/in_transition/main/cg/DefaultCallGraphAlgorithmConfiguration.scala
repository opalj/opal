/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.br.analyses.SomeProject

/**
 * Configuration of a call graph
 * import org.opalj.ai.analyses.cg.CallGraphAlgorithmConfiguration algorithm that uses a cache that depends on the
 * current [[org.opalj.br.MethodSignature]].
 *
 * ==Thread Safety==
 * This class is thread-safe (it contains no mutable state.)
 *
 * ==Usage==
 * Instances of this class are passed to a `CallGraphFactory`'s `create` method.
 *
 * @author Michael Eichberg
 */
abstract class DefaultCallGraphAlgorithmConfiguration(
        val project: SomeProject
) extends CallGraphAlgorithmConfiguration {

    protected type Contour = MethodSignature

    protected type Value = scala.collection.Set[Method]

    protected type Cache = CallGraphCache[Contour, Value]

    protected[this] val cache: Cache = new CallGraphCache[Contour, Value](project)

}
