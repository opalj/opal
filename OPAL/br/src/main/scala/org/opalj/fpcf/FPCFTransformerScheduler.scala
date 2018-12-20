/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.br.analyses.SomeProject

/**
 *  The underlying analysis will only be registered with the property store and
 *  called by the store when a final property of kind `sourcePK` for an entity of type E
 *  is computed.
 *
 * @author Michael Eichberg
 */
trait FPCFTransformerScheduler extends FPCFLazyLikeAnalysisScheduler {

    final override def computationType: ComputationType = Transformer

}

trait BasicFPCFTransformerScheduler extends FPCFTransformerScheduler {
    override type InitializationData = Null
    def init(p: SomeProject, ps: PropertyStore): Null = null
    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}
    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
