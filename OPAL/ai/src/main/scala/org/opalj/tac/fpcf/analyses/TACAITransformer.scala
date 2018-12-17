/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.BasicFPCFTransformerScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.tac.fpcf.properties.NoTACAI
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Transforms an aiResult to the 3-address code.
 *
 * @author Michael Eichberg
 */
object TACAITransformer extends BasicFPCFTransformerScheduler {

    def derivedProperty: PropertyBounds = PropertyBounds.finalP(TACAI)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(BaseAIResult))

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        // To compute the TAC, we (at least) need def-use information; hence, we state
        // this as a requirement.
        val key = AIDomainFactoryKey
        p.updateProjectInformationKeyInitializationData(
            key,
            (i: Option[Set[Class[_ <: AnyRef]]]) ⇒ (i match {
                case None               ⇒ Set(classOf[RecordDefUse])
                case Some(requirements) ⇒ requirements + classOf[RecordDefUse]
            }): Set[Class[_ <: AnyRef]]
        )
        null
    }

    override def register(p: SomeProject, ps: PropertyStore, i: Null): Unit = {
        ps.registerTransformer(BaseAIResult.key, TACAI.key) { (e: Entity, baseAIResult) ⇒
            e match {
                case m: Method ⇒
                    FinalEP(
                        e,
                        baseAIResult.aiResult match {
                            case Some(aiResult) ⇒ TACAIAnalysis.computeTheTACAI(m, aiResult)(p)
                            case None           ⇒ NoTACAI
                        }
                    )
            }
        }
    }

}