/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package connector

import java.io.File

import scala.collection.mutable
import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.lattice.Value

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult
import org.opalj.xl.utility.NoAnalysisResult
import org.opalj.xl.detector.CrossLanguageInteraction
import dk.brics.tajs.lattice.PKey
import org.opalj.xl.detector.NativeInteraction

import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.TypeIteratorState
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis

class NativeConnector(override val project: SomeProject) extends FPCFAnalysis with AbstractPointsToAnalysis with AllocationSiteBasedAnalysis {

    case class NativeConnectorState(
            context:                 Context,
            project:                 SomeProject,
            var files:               List[File]                          = null,
            var nativeInteraction:   CrossLanguageInteraction            = null,
            var translatorDependees: Set[EOptionP[Entity, Property]]     = Set.empty,
            var presetValues:        mutable.Map[PKey.StringPKey, Value] = mutable.Map.empty
    ) extends BaseAnalysisState with TypeIteratorState

    def analyzeContext(context: Context): ProperPropertyComputationResult = {

        //var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        implicit val state = NativeConnectorState(context, project)

        def c(analysis: Option[Analysis])(eps: SomeEPS)(implicit state: NativeConnectorState): ProperPropertyComputationResult = {
            state.translatorDependees = state.translatorDependees.filter(dependee => dependee.e != eps.e || dependee.ub.key != eps.ub.key)
            eps match {
                case LBP(NativeInteraction(nativeFunctionCalls)) =>
                    //TODO Translation Process
                    analyze()
                    createResult(null)

                case ep =>
                    state.translatorDependees += ep
                    createResult(null)
            }
        }

        def analyze()(implicit state: NativeConnectorState): Unit = {
            //TODO call llvm
        }

        def createResult(store: Map[Object, Object])(implicit state: NativeConnectorState): ProperPropertyComputationResult = {
            if (state.translatorDependees.isEmpty && store == null && store.isEmpty) {
                Result(context, NoAnalysisResult)

            } else
                InterimResult(
                    state.nativeInteraction,
                    NoAnalysisResult,
                    InterimAnalysisResult(store),
                    state.translatorDependees,
                    c(null)
                )
        }

        propertyStore(context, CrossLanguageInteraction.key) match {
            case LBP(NativeInteraction(nativeFunctionCalls)) =>
                analyze()
                createResult(null)
            case ep =>
                state.translatorDependees += ep
                createResult(null)
        }
    }
}

object TriggeredNativeConnectorScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq()
    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(CrossLanguageInteraction))
    override def triggeredBy: PropertyKey[CrossLanguageInteraction] =
        CrossLanguageInteraction.key

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): NativeConnector = {
        val analysis = new NativeConnector(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyzeContext)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] =
        Set(PropertyBounds.ub(AnalysisResult))
}
