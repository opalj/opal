/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.scheduling

import org.opalj.fpcf.{ComputationType, PropertyStore, Transformer}
import org.opalj.si.{FPCFAnalysis, MetaProject}

/**
 *  The underlying analysis will only be registered with the property store and
 *  called by the store when a final property of kind `sourcePK` for an entity of type E
 *  is computed.
 *
 * @author Michael Eichberg
 */
trait FPCFTransformerScheduler[P <: MetaProject] extends FPCFLazyLikeAnalysisScheduler[P] {

    final override def computationType: ComputationType = Transformer

}

trait BasicFPCFTransformerScheduler[P <: MetaProject] extends FPCFTransformerScheduler[P] {
    override type InitializationData = Null
    override def init(p: P, ps: PropertyStore): Null = null
    override def beforeSchedule(p: P, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        P,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
