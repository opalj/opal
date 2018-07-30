/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheMethod

/**
 * Configuration of a call graph algorithm that uses "variable type analysis".
 *
 * ==Thread Safety==
 * This class is thread-safe
 * import org.opalj.ai.analyses.cg.BasicVTACallGraphDomain
 * import org.opalj.ai.analyses.cg.ExtVTACallGraphDomain
 * import org.opalj.ai.analyses.cg.BasicVTAWithPreAnalysisCallGraphDomain
 * import org.opalj.ai.analyses.cg.DefaultVTACallGraphDomain(it contains no mutable state.)
 *
 * ==Usage==
 * Instances of this class are passed to a `CallGraphFactory`'s `create` method.
 *
 * @author Michael Eichberg
 */
abstract class VTACallGraphAlgorithmConfiguration(
        project: SomeProject
) extends DefaultCallGraphAlgorithmConfiguration(project) {

    type CallGraphDomain = Domain with ReferenceValuesDomain with TheProject with TheMethod

    def Domain(method: Method): CallGraphDomain

    val Extractor = new VTACallGraphExtractor(cache, Domain)
}

class BasicVTACallGraphAlgorithmConfiguration(
        project: SomeProject
) extends VTACallGraphAlgorithmConfiguration(project) {

    def Domain(method: Method): BasicVTACallGraphDomain[_] = {
        new BasicVTACallGraphDomain(project, cache, method)
    }
}

abstract class VTAWithPreAnalysisCallGraphAlgorithmConfiguration(
        project: SomeProject
) extends VTACallGraphAlgorithmConfiguration(project) {

    val fieldValueInformation = project.get(FieldValuesKey)

    val methodReturnValueInformation = project.get(MethodReturnValuesKey)

}

class BasicVTAWithPreAnalysisCallGraphAlgorithmConfiguration(
        project: SomeProject
) extends VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {

    def Domain(method: Method): BasicVTAWithPreAnalysisCallGraphDomain[_] = {
        new BasicVTAWithPreAnalysisCallGraphDomain(
            project, fieldValueInformation, methodReturnValueInformation,
            cache,
            method
        )
    }
}

class DefaultVTACallGraphAlgorithmConfiguration(
        project: SomeProject
) extends VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {

    def Domain(method: Method): DefaultVTACallGraphDomain[_] = {
        new DefaultVTACallGraphDomain(
            project, fieldValueInformation, methodReturnValueInformation,
            cache,
            method
        )
    }
}

class ExtVTACallGraphAlgorithmConfiguration(
        project: SomeProject
) extends VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {

    def Domain(method: Method): ExtVTACallGraphDomain[_] = {
        new ExtVTACallGraphDomain(
            project, fieldValueInformation, methodReturnValueInformation,
            cache,
            method
        )
    }
}
