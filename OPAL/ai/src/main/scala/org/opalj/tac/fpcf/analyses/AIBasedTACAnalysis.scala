/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.EPK
import org.opalj.tac.fpcf.properties.AIBasedTAC

/**
 * Basically just (re)creates the tac of a method if the result of the underlying
 * abstract interpretation changes.
 *
 * @author Michael Eichberg
 */
class AIBasedTACAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    def computeTAC(entity: Entity): PropertyComputationResult = {
        entity match {
            case m: Method ⇒
                computeTAC(m)
            case e ⇒
                val m = "expected org.opalj.br.Method; given "+e.getClass.getSimpleName
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Computes the TAC for the given method.
     */
    private[analyses] def computeTAC(m: Method): PropertyComputationResult = {
        propertyStore(m, BaseAIResult.key) match {
            case epk: EPK[_, _] ⇒ IntermediateResult
        }
    }
}

sealed trait AIBasedTACAnalysisScheduler extends ComputationSpecification {

    final override def uses: Set[PropertyKind] = Set(BaseAIResult)

    final override def derives: Set[PropertyKind] = Set(AIBasedTAC)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = {
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

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerAIBasedTACAnalysis extends AIBasedTACAnalysisScheduler with FPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new AIBasedTACAnalysis(p)
        val methods = p.allMethodsWithBody
        ps.scheduleEagerComputationsForEntities(methods)(analysis.computeTAC)
        analysis
    }
}

object LazyAIBasedTACAnalysis extends AIBasedTACAnalysisScheduler with FPCFLazyAnalysisScheduler {

    final override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new AIBasedTACAnalysis(p)
        ps.registerLazyPropertyComputation(AIBasedTAC.key, analysis.computeTAC)
        analysis
    }
}
