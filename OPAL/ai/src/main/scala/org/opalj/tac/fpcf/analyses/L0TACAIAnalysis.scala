/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.Result
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.AnAIResult
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.fpcf.EPK
import org.opalj.fpcf.IntermediateEP
import org.opalj.fpcf.IntermediateResult
import org.opalj.ai.fpcf.analyses.L0BaseAIResultAnalysis
import org.opalj.ai.fpcf.properties.NoAIResult
import org.opalj.tac.{TACAI ⇒ TACAIFactory}
import org.opalj.tac.fpcf.properties.NoTACAI
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * Basically just (re)creates the tac of a method if the result of the underlying
 * abstract interpretation changes.
 *
 * @author Michael Eichberg
 */
class L0TACAIAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val aiFactory = project.get(AIDomainFactoryKey)

    def computeTAC(e: Entity): PropertyComputationResult = {
        e match { case m: Method ⇒ computeTAC(m) }
    }

    def computeTheTACAI(m: Method, aiResult: AIResult): TheTACAI = {
        val typedAIResult = aiResult.asInstanceOf[AIResult { val domain: Domain with RecordDefUse }]
        val taCode = TACAIFactory(m, p.classHierarchy, typedAIResult)(Nil)
        val tacaiProperty = TheTACAI(
            // the following cast is safe - see TACode for details
            taCode.asInstanceOf[TACode[TACMethodParameter, DUVar[ValueInformation]]]
        )
    }

    /**
     * Computes the TAC for the given method `m`.
     */
    private[analyses] def computeTAC(m: Method): PropertyComputationResult = {
        ps(m, BaseAIResult.key) match {
            case FinalP(_, NoAIResult) ⇒
                Result(m, NoTACAI)

            case FinalP(_, AnAIResult(initialAIResult)) ⇒
                Result(m, computeTheTACAI(m, initialAIResult))

            case currentAIResult @ IntermediateEP(_, AnAIResult(initialLBAIResult), ub) ⇒
                val newLB =
                    computeTheTACAI(m, initialLBAIResult)

                val newUB =
                    if (ub == NoAIResult)
                        NoTACAI
                    else {
                        val AnAIResult(initialUBAIResult) = ub
                        computeTheTACAI(m, initialUBAIResult)
                    }

                IntermediateResult(
                    m,
                    newLB,
                    newUB,
                    List(currentAIResult),
                    c = null
                )

            case epk: EPK[_, _] ⇒
                val aiResult = L0BaseAIResultAnalysis.performAI(aiFactory, m)

                IntermediateResult(
                    m,
                    computeTheTACAI(m, aiResult),
                    NoTACAI,
                    List(epk),
                    c = null
                )

        }

    }
}

sealed trait L0TACAIAnalysisScheduler extends ComputationSpecification {

    final override def uses: Set[PropertyKind] = Set(BaseAIResult)

    final override def derives: Set[PropertyKind] = Set(TACAI)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = {
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

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerL0TACAIAnalysis extends L0TACAIAnalysisScheduler with FPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0TACAIAnalysis(p)
        val methods = p.allMethodsWithBody
        ps.scheduleEagerComputationsForEntities(methods)(analysis.computeTAC)
        analysis
    }
}

object LazyL0TACAIAnalysis extends L0TACAIAnalysisScheduler with FPCFLazyAnalysisScheduler {

    final override def startLazily(
        p: SomeProject, 
        ps: PropertyStore, 
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L0TACAIAnalysis(p)
        ps.registerLazyPropertyComputation(TACAI.key, analysis.computeTAC)
        analysis
    }
}
