/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.ai.fpcf.analyses.L0BaseAIResultAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.AnAIResult
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.ai.fpcf.properties.NoAIResult
import org.opalj.ai.fpcf.properties.ProjectSpecificAIExecutor
import org.opalj.tac.fpcf.properties.NoTACAI
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Basically just (re)creates the tac of a method if the result of the underlying
 * abstract interpretation changes.
 *
 * @author Michael Eichberg
 */
class L0TACAIAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    import org.opalj.tac.fpcf.analyses.TACAIAnalysis.computeTheTACAI

    final implicit val aiFactory: ProjectSpecificAIExecutor = project.get(AIDomainFactoryKey)

    def computeTAC(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: Method => computeTAC(m)
            case _         => throw new IllegalArgumentException(s"$e is not a method")
        }
    }

    /**
     * Computes the TAC for the given method `m`.
     */
    private[analyses] def computeTAC(m: Method): ProperPropertyComputationResult = {
        c(ps[Method, BaseAIResult](m, BaseAIResult.key))
    }

    def c(eOptionP: EOptionP[Method, BaseAIResult]): ProperPropertyComputationResult = {
        val m = eOptionP.e
        (eOptionP: @unchecked) match {
            case FinalP(NoAIResult)           => Result(m, NoTACAI)
            case FinalP(AnAIResult(aiResult)) => Result(m, computeTheTACAI(m, aiResult, false))

            case currentAIResult @ InterimLUBP(AnAIResult(initialLBAIResult), ub) =>
                val newLB = computeTheTACAI(m, initialLBAIResult, false)
                val newUB =
                    if (ub == NoAIResult)
                        NoTACAI
                    else {
                        val AnAIResult(initialUBAIResult) = ub
                        computeTheTACAI(m, initialUBAIResult, false)
                    }

                InterimResult.create(
                    m,
                    newLB,
                    newUB,
                    Set(currentAIResult),
                    c = c
                )

            case epk @ EPK(m: Method, _) =>
                val aiResult = L0BaseAIResultAnalysis.performAI(m)

                InterimResult.create(
                    m,
                    computeTheTACAI(m, aiResult, false),
                    NoTACAI,
                    Set(epk),
                    c = c
                )
        }
    }
}

sealed trait L0TACAIAnalysisScheduler extends TACAIInitializer {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(AIDomainFactoryKey)

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(BaseAIResult))

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(TACAI)

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

}

object EagerL0TACAIAnalysis extends L0TACAIAnalysisScheduler with FPCFEagerAnalysisScheduler {

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0TACAIAnalysis(p)
        val methods = p.allMethodsWithBody
        ps.scheduleEagerComputationsForEntities(methods)(analysis.computeTAC)
        analysis
    }
}

object LazyL0TACAIAnalysis extends L0TACAIAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0TACAIAnalysis(p)
        ps.registerLazyPropertyComputation(TACAI.key, analysis.computeTAC)
        analysis
    }
}
