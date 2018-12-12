/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.AbstractFPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.analyses.L0BaseAIResultAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.AnAIResult
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.ai.fpcf.properties.NoAIResult
import org.opalj.ai.fpcf.properties.ProjectSpecificAIExecutor
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

    import TACAIAnalysis.computeTheTACAI

    final implicit val aiFactory: ProjectSpecificAIExecutor = project.get(AIDomainFactoryKey)

    def computeTAC(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: Method ⇒ computeTAC(m)
            case _         ⇒ throw new IllegalArgumentException(s"$e is not a method")
        }
    }

    /**
     * Computes the TAC for the given method `m`.
     */
    private[analyses] def computeTAC(m: Method): ProperPropertyComputationResult = {
        ps(m, BaseAIResult.key) match {
            case FinalP(NoAIResult)           ⇒ Result(m, NoTACAI)

            case FinalP(AnAIResult(aiResult)) ⇒ Result(m, computeTheTACAI(m, aiResult))

            case currentAIResult @ InterimLUBP(AnAIResult(initialLBAIResult), ub) ⇒
                val newLB = computeTheTACAI(m, initialLBAIResult)

                val newUB =
                    if (ub == NoAIResult)
                        NoTACAI
                    else {
                        val AnAIResult(initialUBAIResult) = ub
                        computeTheTACAI(m, initialUBAIResult)
                    }

                InterimResult(
                    m,
                    newLB,
                    newUB,
                    List(currentAIResult),
                    c = null
                )

            case epk: EPK[_, _] ⇒
                val aiResult = L0BaseAIResultAnalysis.performAI(m)

                InterimResult(
                    m,
                    computeTheTACAI(m, aiResult),
                    NoTACAI,
                    List(epk),
                    c = null
                )

        }

    }
}

object TACAIAnalysis {

    def computeTheTACAI(m: Method, aiResult: AIResult)(implicit p: SomeProject): TheTACAI = {
        val typedAIResult = aiResult.asInstanceOf[AIResult { val domain: Domain with RecordDefUse }]
        val taCode = TACAIFactory(m, p.classHierarchy, typedAIResult)(Nil)
        val tacaiProperty = TheTACAI(
            // the following cast is safe - see TACode for details
            taCode.asInstanceOf[TACode[TACMethodParameter, DUVar[ValueInformation]]]
        )
        tacaiProperty
    }

}

sealed trait L0TACAIAnalysisScheduler extends AbstractFPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(BaseAIResult))

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(TACAI)

    override type InitializationData = Null
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

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

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

    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0TACAIAnalysis(p)
        ps.registerLazyPropertyComputation(TACAI.key, analysis.computeTAC)
        analysis
    }
}
