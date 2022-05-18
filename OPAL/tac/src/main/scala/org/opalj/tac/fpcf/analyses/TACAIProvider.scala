/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.fpcf.analyses.DomainBasedFPCFAnalysisScheduler
import org.opalj.ai.fpcf.analyses.L0BaseAIResultAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.ProjectSpecificAIExecutor
import org.opalj.tac.fpcf.analyses.TACAIAnalysis.computeTheTACAI
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Provides the TACAI for all methods. The TACAI provided by the TACAI provider is always
 * detached from the underlying results of the abstract interpretation and therefore
 * significantly reduces the overall memory consumption if the AIResults are not needed!
 *
 * @author Michael Eichberg
 */
class TACAIProvider private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final implicit val aiFactory: ProjectSpecificAIExecutor = project.get(AIDomainFactoryKey)

    def computeTAC(e: Entity): ProperPropertyComputationResult = e match {
        case m: Method =>
            val aiResult = L0BaseAIResultAnalysis.performAI(m)
            Result(FinalEP(m, computeTheTACAI(m, aiResult, detachFromAIResult = true)))
    }

}

sealed trait TACAIProviderScheduler extends TACAIInitializer with DomainBasedFPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(AIDomainFactoryKey)

    final def derivedProperty: PropertyBounds = PropertyBounds.finalP(TACAI)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

object EagerTACAIProvider extends TACAIProviderScheduler with FPCFEagerAnalysisScheduler {

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new TACAIProvider(p) // THE TACAIPROVIDER MUST NOT BE CREATED IN INIT!
        val methods = p.allMethodsWithBody
        ps.scheduleEagerComputationsForEntities(methods)(analysis.computeTAC)
        analysis
    }
}

object LazyTACAIProvider extends TACAIProviderScheduler with FPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new TACAIProvider(p) // THE TACAIPROVIDER MUST NOT BE CREATED IN INIT!
        ps.registerLazyPropertyComputation(TACAI.key, analysis.computeTAC)
        analysis
    }
}
