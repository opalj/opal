/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package trivial

import org.opalj.br.Method
import org.opalj.br.PDVar
import org.opalj.br.PUVar
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeConst
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Provides only the most trivial information about available strings for a given variable.
 *
 * If the variable represents a constant string, the string value will be captured and returned in the result.
 * If the variable represents any other value, no string value can be derived and the analysis returns either the upper
 * or lower bound depending on the soundness mode.
 *
 * @author Maximilian Rüsch
 *
 * @see [[StringAnalysis]]
 */
class TrivialStringAnalysis(override val project: SomeProject) extends FPCFAnalysis {

    private final val ConfigLogCategory = "analysis configuration - trivial string analysis"

    private val soundnessMode: SoundnessMode = {
        val mode =
            try {
                SoundnessMode(project.config.getBoolean(TrivialStringAnalysis.SoundnessModeConfigKey))
            } catch {
                case t: Throwable =>
                    logOnce {
                        Error(ConfigLogCategory, s"couldn't read: ${TrivialStringAnalysis.SoundnessModeConfigKey}", t)
                    }
                    SoundnessMode(false)
            }

        logOnce(Info(ConfigLogCategory, "using soundness mode " + mode))
        mode
    }

    private case class TrivialStringAnalysisState(entity: VariableContext, var tacDependee: EOptionP[Method, TACAI])

    def analyze(variableContext: VariableContext): ProperPropertyComputationResult = {
        implicit val state: TrivialStringAnalysisState = TrivialStringAnalysisState(
            variableContext,
            ps(variableContext.m, TACAI.key)
        )

        if (state.tacDependee.isRefinable) {
            InterimResult(
                state.entity,
                StringConstancyProperty.lb,
                StringConstancyProperty.ub,
                Set(state.tacDependee),
                continuation(state)
            )
        } else if (state.tacDependee.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            Result(state.entity, StringConstancyProperty(failure))
        } else {
            determinePossibleStrings
        }
    }

    private def continuation(state: TrivialStringAnalysisState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case tacaiEPS: EPS[_, _] if eps.pk == TACAI.key =>
                state.tacDependee = tacaiEPS.asInstanceOf[EPS[Method, TACAI]]
                determinePossibleStrings(state)

            case _ =>
                throw new IllegalArgumentException(s"Unknown EPS given in continuation: $eps")
        }
    }

    private def determinePossibleStrings(
        implicit state: TrivialStringAnalysisState
    ): ProperPropertyComputationResult = {
        val tac = state.tacDependee.ub.tac.get

        def mapDefPCToStringTree(defPC: Int): StringTreeNode = {
            if (defPC < 0) {
                failure
            } else {
                tac.stmts(valueOriginOfPC(defPC, tac.pcToIndex).get).asAssignment.expr match {
                    case StringConst(_, v) => StringTreeConst(v)
                    case _                 => failure
                }
            }
        }

        val tree = state.entity.pv match {
            case PUVar(_, defPCs) =>
                StringTreeOr(defPCs.map(pc => mapDefPCToStringTree(pc)))

            case PDVar(_, _) =>
                mapDefPCToStringTree(state.entity.pc)
        }

        Result(state.entity, StringConstancyProperty(tree))
    }

    private def failure: StringTreeNode = {
        if (soundnessMode.isHigh) StringTreeNode.lb
        else StringTreeNode.ub
    }
}

private object TrivialStringAnalysis {

    private final val SoundnessModeConfigKey = "org.opalj.fpcf.analyses.string.TrivialStringAnalysis.highSoundness"
}

/**
 * @author Maximilian Rüsch
 *
 * @see [[TrivialStringAnalysis]]
 */
object LazyTrivialStringAnalysis extends BasicFPCFLazyAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(TACAI)

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.lub(StringConstancyProperty))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(ContextProviderKey)

    override def register(project: SomeProject, propertyStore: PropertyStore, i: InitializationData): FPCFAnalysis = {
        val analysis = new TrivialStringAnalysis(project)
        propertyStore.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }
}
