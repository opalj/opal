/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.fpcf.ComputationType
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Transformer
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
    override def init(p: SomeProject, ps: PropertyStore): Null = null
    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
