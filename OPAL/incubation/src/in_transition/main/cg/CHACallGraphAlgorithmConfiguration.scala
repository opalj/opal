/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.br.analyses.SomeProject

/**
 * Configuration of a call graph algorithm that uses CHA.
 *
 * ==Thread Safety==
 * This class is thread-safe (it contains no mutable state.)
 *
 * ==Usage==
 * Instances of this class are passed to a `CallGraphFactory`'s `create` method.
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
class CHACallGraphAlgorithmConfiguration(
        project:                       SomeProject,
        withCallBySignatureResolution: Boolean     = false
) extends DefaultCallGraphAlgorithmConfiguration(project) {

    final val Extractor = initExtractor

    final def initExtractor: CallGraphExtractor =
        if (withCallBySignatureResolution)
            new CHACallGraphExtractorWithCBS(cache)
        else
            new CHACallGraphExtractor(cache)
}

