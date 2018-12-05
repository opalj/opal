/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.AnAIResult
import org.opalj.ai.fpcf.properties.BaseAIResult
import org.opalj.ai.fpcf.properties.ProjectSpecificAIExecutor

/**
 * Performs an abstract interpretation of a method using a project's AIDomainFactoryKey.
 *
 * @author Michael Eichberg
 */
class L0BaseAIResultAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final implicit val aiFactory: ProjectSpecificAIExecutor = project.get(AIDomainFactoryKey)

    def performAI(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case m: Method ⇒ Result(m, AnAIResult(L0BaseAIResultAnalysis.performAI(m)))
            case e         ⇒ throw new IllegalArgumentException(s"$e is not a method")
        }
    }
}

object L0BaseAIResultAnalysis {

    /**
     * Performs the abstract interpretation of the given method.
     *
     * @param m The method that is analyzed. If `m` contains invalid bytecode, a new
     *          fake method is created that always just throws an error, where the
     *          message contains some details about the problem of the underlying/original
     *          bytecode.
     */
    def performAI(
        m: Method
    )(
        implicit
        aiFactory:  ProjectSpecificAIExecutor,
        logContext: LogContext
    ): AIResult = {
        try {
            aiFactory(m)
        } catch {
            case t: Throwable ⇒
                error(
                    "project configuration",
                    s"interpretation of ${m.toJava} failed; "+
                        " replacing method body with a generic error throwing body",
                    t
                )
                val reason = Some("replaced due to invalid bytecode\n"+t.getMessage)
                performAI(m.invalidBytecode(reason))
        }
    }
}

sealed trait L0BaseAIResultAnalysisScheduler extends ComputationSpecification {

    final override def uses: Set[PropertyKind] = Set()

    final override def derives: Set[PropertyKind] = Set(BaseAIResult)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null
    // TODO Add requirement that we want to have a domain that interacts with "Value" information

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerL0BaseAIAnalysis
    extends L0BaseAIResultAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0BaseAIResultAnalysis(p)
        val methods = p.allMethodsWithBody
        ps.scheduleEagerComputationsForEntities(methods)(analysis.performAI)
        analysis
    }
}

object LazyL0BaseAIResultAnalysis
    extends L0BaseAIResultAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    final override def startLazily(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L0BaseAIResultAnalysis(p)
        ps.registerLazyPropertyComputation(BaseAIResult.key, analysis.performAI)
        analysis
    }
}
