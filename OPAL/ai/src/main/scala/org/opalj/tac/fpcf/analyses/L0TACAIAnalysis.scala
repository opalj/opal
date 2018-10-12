/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.MultiResult
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.value.KnownTypedValue
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.AnAIResult
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.tac.{TACAI ⇒ TACAIFactory}
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
        val aiResult = aiFactory(m).asInstanceOf[AIResult { val domain: Domain with RecordDefUse }]
        val aiResultProperty = AnAIResult(aiResult)
        val taCode = TACAIFactory(m, p.classHierarchy, aiResult)(Nil)
        val tacaiProperty = TheTACAI(
            // the following cast is safe - see TACode for details
            // IMPROVE Get rid of nasty type checks/casts related to TACode once we use ConstCovariantArray in TACode.. (here and elsewhere)
            taCode.asInstanceOf[TACode[TACMethodParameter, DUVar[KnownTypedValue]]]
        )
        MultiResult(List(FinalEP(m, aiResultProperty), FinalEP(m, tacaiProperty)))
    }
}

sealed trait L0TACAIAnalysisScheduler extends ComputationSpecification {

    final override def uses: Set[PropertyKind] = Set()

    final override def derives: Set[PropertyKind] = Set(BaseAIResult, TACAI)

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

    final override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0TACAIAnalysis(p)
        ps.registerLazyMultiPropertyComputation(analysis.computeTAC, BaseAIResult.key, TACAI.key)
        analysis
    }
}
