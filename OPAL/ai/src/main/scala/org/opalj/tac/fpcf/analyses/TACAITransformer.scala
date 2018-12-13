/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.BasicFPCFTransformerScheduler
import org.opalj.fpcf.DefaultFPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.tac.fpcf.properties.NoTACAI
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Transforms an aiResult to the 3-address code.
 *
 * @author Michael Eichberg
 */
object TACAITransformer extends BasicFPCFTransformerScheduler with TACAIInitializer {

    def derivedProperty: PropertyBounds = PropertyBounds.finalP(TACAI)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(BaseAIResult))

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, i: Null): FPCFAnalysis = {
        class TheTACAITransformer
            extends DefaultFPCFAnalysis(p)
            with ((Entity, BaseAIResult) ⇒ FinalEP[Method, TACAI]) {

            def apply(e: Entity, baseAIResult: BaseAIResult): FinalEP[Method, TACAI] = {
                e match {
                    case m: Method ⇒
                        FinalEP(
                            m,
                            baseAIResult.aiResult match {
                                case Some(aiResult) ⇒ TACAIAnalysis.computeTheTACAI(m, aiResult)(p)
                                case None           ⇒ NoTACAI
                            }
                        )
                }
            }
        }
        val analysis = new TheTACAITransformer
        ps.registerTransformer(BaseAIResult.key, TACAI.key) { analysis }
        analysis
    }

}
